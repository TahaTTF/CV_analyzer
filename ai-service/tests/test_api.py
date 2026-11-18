from fastapi.testclient import TestClient
import app.main as main
from app.matcher import HybridMatcher
from test_matcher import FakeEmbedder, sample

client=TestClient(main.app)

def test_health():
    response=client.get("/health")
    assert response.status_code==200 and response.json()["status"]=="UP"
    assert "embeddingCache" in response.json()

def test_invalid_request():
    assert client.post("/match",json={"semantic_profile":"","jobs":[]}).status_code==422

def test_match_contract_uses_camel_case(monkeypatch):
    monkeypatch.setattr(main,"matcher",HybridMatcher(FakeEmbedder()))
    response=client.post("/match",json=sample(1).model_dump(by_alias=True))
    assert response.status_code==200
    assert response.json()["recommendations"][0]["jobId"]=="a"
    assert "semanticScore" in response.json()["recommendations"][0]
