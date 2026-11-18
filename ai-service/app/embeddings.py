import threading
import time

import numpy as np

MODEL_NAME = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"

class SentenceTransformerEmbedder:
    def __init__(self, model_name: str = MODEL_NAME):
        self.model_name = model_name
        self._model = None
        self._model_lock = threading.RLock()
        self.model_load_seconds = 0.0

    @property
    def model(self):
        if self._model is None:
            with self._model_lock:
                if self._model is None:
                    from sentence_transformers import SentenceTransformer
                    started = time.perf_counter()
                    self._model = SentenceTransformer(self.model_name, device="cpu")
                    self.model_load_seconds = time.perf_counter() - started
        return self._model

    def encode(self, texts: list[str]) -> np.ndarray:
        if not texts:
            return np.empty((0, 0), dtype=np.float32)
        with self._model_lock:
            return np.asarray(self.model.encode(texts, batch_size=32, normalize_embeddings=True,
                show_progress_bar=False), dtype=np.float32)

def cosine_scores(reference: np.ndarray, candidates: np.ndarray) -> np.ndarray:
    return np.clip((candidates @ reference + 1.0) * 50.0, 0.0, 100.0)
