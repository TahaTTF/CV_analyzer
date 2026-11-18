from collections import OrderedDict
from dataclasses import dataclass
import hashlib
import json
import threading
import time

import numpy as np


PREPROCESSING_VERSION = "job-text-v1"


@dataclass(frozen=True)
class CacheLookup:
    vectors: np.ndarray
    hits: int
    misses: int
    embeddings_computed: int
    embedding_seconds: float


@dataclass(frozen=True)
class CacheEntry:
    job_id: str
    vector: np.ndarray


class JobEmbeddingCache:
    """Bounded process-local LRU cache for normalized job embedding inputs."""

    def __init__(self, max_entries: int = 5_000):
        if max_entries < 1:
            raise ValueError("max_entries must be positive")
        self.max_entries = max_entries
        self._entries: OrderedDict[str, CacheEntry] = OrderedDict()
        self._key_by_job_id: dict[str, str] = {}
        self._lock = threading.RLock()
        self._requests = 0
        self._hits = 0
        self._misses = 0
        self._embeddings_computed = 0
        self._embedding_seconds = 0.0
        self._invalidations = 0
        self._evictions = 0

    @staticmethod
    def stable_key(job_id: str, text: str, model_identity: str,
                   preprocessing_version: str = PREPROCESSING_VERSION) -> str:
        payload = json.dumps(
            [model_identity, preprocessing_version, job_id, text],
            ensure_ascii=False,
            separators=(",", ":"),
        ).encode("utf-8")
        return hashlib.sha256(payload).hexdigest()

    def get_or_encode(self, jobs: list[tuple[str, str]], embedder,
                      model_identity: str,
                      preprocessing_version: str = PREPROCESSING_VERSION) -> CacheLookup:
        if not jobs:
            return CacheLookup(np.empty((0, 0), dtype=np.float32), 0, 0, 0, 0.0)

        with self._lock:
            self._requests += 1
            keys = [self.stable_key(job_id, text, model_identity, preprocessing_version)
                    for job_id, text in jobs]
            request_hits = 0
            missing_indexes: list[int] = []
            vectors_by_key: dict[str, np.ndarray] = {}
            for index, key in enumerate(keys):
                if key in self._entries:
                    request_hits += 1
                    self._entries.move_to_end(key)
                    vectors_by_key[key] = self._entries[key].vector
                else:
                    missing_indexes.append(index)

            request_misses = len(missing_indexes)
            embedding_seconds = 0.0
            if missing_indexes:
                started = time.perf_counter()
                encoded = np.asarray(embedder.encode([jobs[index][1] for index in missing_indexes]),
                                     dtype=np.float32)
                embedding_seconds = time.perf_counter() - started
                if encoded.ndim != 2 or encoded.shape[0] != len(missing_indexes):
                    raise ValueError("Embedder returned an invalid job embedding matrix")
                for row, index in zip(encoded, missing_indexes):
                    job_id, _ = jobs[index]
                    key = keys[index]
                    old_key = self._key_by_job_id.get(job_id)
                    if old_key is not None and old_key != key:
                        if self._entries.pop(old_key, None) is not None:
                            self._invalidations += 1
                    vector = np.asarray(row, dtype=np.float32).copy()
                    vector.setflags(write=False)
                    self._entries[key] = CacheEntry(job_id, vector)
                    self._key_by_job_id[job_id] = key
                    vectors_by_key[key] = vector
                    self._entries.move_to_end(key)
                    self._evict_if_needed()

            self._hits += request_hits
            self._misses += request_misses
            self._embeddings_computed += request_misses
            self._embedding_seconds += embedding_seconds
            vectors = np.vstack([vectors_by_key[key] for key in keys])
            return CacheLookup(vectors, request_hits, request_misses, request_misses,
                               embedding_seconds)

    def remove(self, job_id: str) -> bool:
        """Explicit hook for a future deletion event; matching never returns absent request jobs."""
        with self._lock:
            key = self._key_by_job_id.pop(job_id, None)
            return key is not None and self._entries.pop(key, None) is not None

    def clear(self) -> None:
        with self._lock:
            self._entries.clear()
            self._key_by_job_id.clear()

    def snapshot(self) -> dict[str, int | float]:
        with self._lock:
            total = self._hits + self._misses
            return {
                "entries": len(self._entries),
                "maxEntries": self.max_entries,
                "hits": self._hits,
                "misses": self._misses,
                "hitRate": round(self._hits / total, 4) if total else 0.0,
                "embeddingsComputed": self._embeddings_computed,
                "embeddingSeconds": round(self._embedding_seconds, 6),
                "invalidations": self._invalidations,
                "evictions": self._evictions,
                "vectorBytes": sum(entry.vector.nbytes for entry in self._entries.values()),
                "requests": self._requests,
            }

    def _evict_if_needed(self) -> None:
        while len(self._entries) > self.max_entries:
            key, entry = self._entries.popitem(last=False)
            if self._key_by_job_id.get(entry.job_id) == key:
                del self._key_by_job_id[entry.job_id]
            self._evictions += 1
