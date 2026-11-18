from concurrent.futures import ThreadPoolExecutor
import threading
import time

import numpy as np

from app.job_cache import JobEmbeddingCache
from app.matcher import HybridMatcher
from app.models import MatchRequest


class CountingEmbedder:
    model_name = "deterministic-test-model"

    def __init__(self, delay: float = 0.0):
        self.calls = 0
        self.encoded_texts = 0
        self.delay = delay
        self._lock = threading.Lock()

    def encode(self, texts):
        with self._lock:
            self.calls += 1
            self.encoded_texts += len(texts)
        if self.delay:
            time.sleep(self.delay)
        vectors = []
        for text in texts:
            lowered = text.lower()
            vector = np.asarray([
                1.0 if "data" in lowered or "python" in lowered else 0.2,
                1.0 if "sales" in lowered or "business" in lowered else 0.1,
                1.0 if "cloud" in lowered else 0.3,
            ], dtype=np.float32)
            vectors.append(vector / np.linalg.norm(vector))
        return np.asarray(vectors, dtype=np.float32)


def request(jobs=None):
    return MatchRequest(
        semantic_profile="Data engineer with Python and cloud experience",
        skills=["Python", "PostgreSQL"],
        target_role="Data Engineer",
        top_k=10,
        jobs=[
            {"job_id": "data-1", "title": "Data Engineer", "description": "Python pipelines",
             "skills": ["Python", "PostgreSQL"], "categories": ["data"]},
            {"job_id": "sales-1", "title": "Sales Manager", "description": "Business accounts",
             "skills": [], "categories": ["sales"]},
        ] if jobs is None else jobs,
    )


def test_cold_then_warm_cache_reuses_unchanged_job_embeddings():
    embedder = CountingEmbedder()
    matcher = HybridMatcher(embedder=embedder, cache=JobEmbeddingCache(max_entries=10))

    cold = matcher.match(request())
    warm = matcher.match(request())

    assert cold.recommendations == warm.recommendations
    assert embedder.calls == 3  # CV + cold jobs, then CV only.
    assert matcher.diagnostics()["hits"] == 2
    assert matcher.diagnostics()["embeddingsComputed"] == 2


def test_content_change_and_model_version_invalidate_stale_vector():
    embedder = CountingEmbedder()
    cache = JobEmbeddingCache(max_entries=10)
    jobs = [("job-1", "Data Engineer. Python")]

    cache.get_or_encode(jobs, embedder, "model-v1")
    cache.get_or_encode(jobs, embedder, "model-v1")
    cache.get_or_encode([("job-1", "Data Engineer. Python and cloud")], embedder, "model-v1")
    cache.get_or_encode([("job-1", "Data Engineer. Python and cloud")], embedder, "model-v2")

    stats = cache.snapshot()
    assert stats["hits"] == 1
    assert stats["misses"] == 3
    assert stats["invalidations"] == 2
    assert stats["entries"] == 1


def test_new_deleted_empty_and_invalid_jobs_are_safe():
    embedder = CountingEmbedder()
    cache = JobEmbeddingCache(max_entries=10)
    lookup = cache.get_or_encode([("new", ""), ("malformed", "   ")], embedder, "model-v1")

    assert lookup.vectors.shape == (2, 3)
    assert cache.remove("new") is True
    assert cache.remove("deleted-or-unknown") is False
    assert cache.snapshot()["entries"] == 1
    assert HybridMatcher(embedder=embedder, cache=cache).match(request(jobs=[])).recommendations == []


def test_lru_cache_is_bounded_and_tracks_evictions():
    cache = JobEmbeddingCache(max_entries=2)
    lookup = cache.get_or_encode(
        [("one", "Data one"), ("two", "Data two"), ("three", "Data three")],
        CountingEmbedder(), "model-v1")

    assert lookup.vectors.shape == (3, 3)
    assert cache.snapshot()["entries"] == 2
    assert cache.snapshot()["evictions"] == 1


def test_concurrent_requests_compute_each_job_once():
    embedder = CountingEmbedder(delay=0.02)
    matcher = HybridMatcher(embedder=embedder, cache=JobEmbeddingCache(max_entries=10))

    with ThreadPoolExecutor(max_workers=4) as executor:
        responses = list(executor.map(lambda _: matcher.match(request()), range(4)))

    assert all(response.recommendations == responses[0].recommendations for response in responses)
    stats = matcher.diagnostics()
    assert stats["embeddingsComputed"] == 2
    assert stats["misses"] == 2
    assert stats["hits"] == 6


def test_stable_key_is_deterministic_and_versioned():
    first = JobEmbeddingCache.stable_key("id", "normalized text", "model", "prep-v1")
    assert first == JobEmbeddingCache.stable_key("id", "normalized text", "model", "prep-v1")
    assert first != JobEmbeddingCache.stable_key("id", "changed", "model", "prep-v1")
    assert first != JobEmbeddingCache.stable_key("id", "normalized text", "model", "prep-v2")
