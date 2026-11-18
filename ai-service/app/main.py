from fastapi import FastAPI
from .matcher import HybridMatcher, MODEL_VERSION
from .models import MatchRequest, MatchResponse

app = FastAPI(title="CV Analyzer AI Matching Service", version="1.0.0")
matcher = HybridMatcher()

@app.get("/health")
def health() -> dict[str, object]:
    return {"status": "UP", "modelVersion": MODEL_VERSION, "embeddingCache": matcher.diagnostics()}

@app.post("/match", response_model=MatchResponse)
def match(request: MatchRequest) -> MatchResponse:
    return matcher.match(request)
