import logging
import os
import threading
import time
import unicodedata

import numpy as np

from .embeddings import SentenceTransformerEmbedder, cosine_scores
from .job_cache import JobEmbeddingCache, PREPROCESSING_VERSION
from .models import MatchRequest, MatchResponse, MatchResult
from .roles import explicit_role_mismatch, role_score
from .skills import compare_skills

MODEL_VERSION = "hybrid-multilingual-minilm-v2"
logger = logging.getLogger("uvicorn.error")

def normalize_embedding_text(value: str) -> str:
    return " ".join(unicodedata.normalize("NFKC", value or "").split())

def job_embedding_text(job) -> str:
    categories = " ".join(normalize_embedding_text(value) for value in job.categories)
    values = [normalize_embedding_text(job.title), normalize_embedding_text(job.description), categories]
    return ". ".join(value for value in values if value)

def explanation(matched: list[str], missing: list[str], semantic: float,
                role: float, has_target_role: bool, explicit_mismatch: bool) -> str:
    parts = []
    if matched:
        parts.append("Matched skills: " + ", ".join(matched[:8]) + ".")
    if missing:
        parts.append("Job skills not identified in the CV: " + ", ".join(missing[:8]) + ".")
    if not matched and not missing:
        parts.append("The job does not provide explicit normalized skill requirements.")
    parts.append("Semantic profile alignment is " +
                 ("strong." if semantic >= 70 else "moderate." if semantic >= 50 else "limited."))
    if has_target_role:
        parts.append("Role-family compatibility is " +
                     ("strong." if role >= 75 else "partial." if role >= 50 else "limited."))
    if explicit_mismatch:
        parts.append("The job title indicates a different professional role family.")
    return " ".join(parts)

class HybridMatcher:
    def __init__(self, embedder=None, semantic_weight=.60, skill_weight=.30, role_weight=.10,
                 cache=None):
        self.embedder = embedder or SentenceTransformerEmbedder()
        self.weights = semantic_weight, skill_weight, role_weight
        self.cache = cache or JobEmbeddingCache(
            max_entries=int(os.getenv("JOB_EMBEDDING_CACHE_MAX_ENTRIES", "5000")))
        self._metrics_lock = threading.Lock()
        self._last_match: dict[str, int | float] = {}

    def match(self, request: MatchRequest) -> MatchResponse:
        if not request.jobs:
            return MatchResponse(model_version=MODEL_VERSION, recommendations=[])
        started = time.perf_counter()
        cv_started = time.perf_counter()
        cv_vectors = np.asarray(self.embedder.encode([request.semantic_profile]), dtype=np.float32)
        cv_seconds = time.perf_counter() - cv_started
        if cv_vectors.ndim != 2 or cv_vectors.shape[0] != 1:
            raise ValueError("Embedder returned an invalid CV embedding matrix")
        texts = [job_embedding_text(job) for job in request.jobs]
        model_identity = getattr(self.embedder, "model_name", self.embedder.__class__.__name__)
        cache_started = time.perf_counter()
        lookup = self.cache.get_or_encode(
            [(job.job_id, text) for job, text in zip(request.jobs, texts)],
            self.embedder,
            str(model_identity),
            PREPROCESSING_VERSION,
        )
        cache_seconds = time.perf_counter() - cache_started
        semantic_scores = cosine_scores(cv_vectors[0], lookup.vectors)
        scoring_started = time.perf_counter()
        results = []
        sw, kw, rw = self.weights
        for job, semantic in zip(request.jobs, semantic_scores):
            skill, matched, missing = compare_skills(request.skills, job.skills)
            role = role_score(request.target_role, job.title)
            mismatch = explicit_role_mismatch(request.target_role, job.title)
            # A confirmed cross-family title mismatch is bounded by the existing role weight.
            role_penalty = rw * 100.0 if mismatch else 0.0
            score = np.clip(sw * float(semantic) + kw * skill + rw * role - role_penalty,
                            0.0, 100.0)
            results.append(MatchResult(job_id=job.job_id, match_score=round(score, 2),
                semantic_score=round(float(semantic), 2), skill_score=round(skill, 2),
                role_score=round(role, 2), matched_skills=matched, missing_skills=missing,
                explanation=explanation(matched, missing, float(semantic), role,
                                        request.target_role is not None, mismatch)))
        results.sort(key=lambda item: (-item.match_score, item.job_id))
        scoring_seconds = time.perf_counter() - scoring_started
        elapsed = time.perf_counter() - started
        cache_state = self.cache.snapshot()
        request_metrics: dict[str, int | float] = {
            "jobs": len(request.jobs),
            "topK": request.top_k,
            "totalMs": round(elapsed * 1000, 3),
            "cvEmbeddingMs": round(cv_seconds * 1000, 3),
            "jobCacheLookupMs": round(cache_seconds * 1000, 3),
            "jobEmbeddingMs": round(lookup.embedding_seconds * 1000, 3),
            "scoringAndRankingMs": round(scoring_seconds * 1000, 3),
            "cacheHits": lookup.hits,
            "cacheMisses": lookup.misses,
        }
        with self._metrics_lock:
            self._last_match = request_metrics
        logger.info(
            "matching_complete jobs=%d top_k=%d total_ms=%.1f cv_embedding_ms=%.1f "
            "cache_hits=%d cache_misses=%d embeddings_computed=%d job_embedding_ms=%.1f "
            "cache_entries=%d",
            len(request.jobs), request.top_k, elapsed * 1000, cv_seconds * 1000,
            lookup.hits, lookup.misses, lookup.embeddings_computed,
            lookup.embedding_seconds * 1000, cache_state["entries"],
        )
        return MatchResponse(model_version=MODEL_VERSION, recommendations=results[:request.top_k])

    def diagnostics(self) -> dict[str, object]:
        values: dict[str, object] = dict(self.cache.snapshot())
        values["preprocessingVersion"] = PREPROCESSING_VERSION
        values["modelLoadSeconds"] = round(
            float(getattr(self.embedder, "model_load_seconds", 0.0)), 6)
        with self._metrics_lock:
            values["lastMatch"] = dict(self._last_match)
        return values
