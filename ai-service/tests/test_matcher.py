import numpy as np
import json
from pathlib import Path
from app.matcher import HybridMatcher, role_score
from app.models import MatchRequest
from app.roles import explicit_role_mismatch
from app.skills import compare_skills, normalize_skill

class FakeEmbedder:
    def encode(self, texts):
        return np.asarray([[1.,0.] if "python" in text.lower() or "data" in text.lower()
                           else [0.,1.] for text in texts], dtype=np.float32)

def sample(top_k=10):
    return MatchRequest(semantic_profile="Python data engineer", skills=["Py","Postgres"],
      target_role="Data Engineer", top_k=top_k, jobs=[
       {"job_id":"b","title":"Sales Manager","description":"accounts","skills":["Excel"]},
       {"job_id":"a","title":"Data Engineer","description":"Python pipelines",
        "skills":["Python","PostgreSQL","Spark"]}])

def test_aliases_are_exact_and_normalized():
    assert normalize_skill("NodeJS") == "node.js"
    score,matched,missing=compare_skills(["JS","Py","Postgres"],
      ["JavaScript","Python","PostgreSQL","Java"])
    assert score == 75.0 and matched == ["javascript","postgresql","python"] and missing == ["java"]
    assert compare_skills(["Java"],["JavaScript"])[1] == []

def test_empty_job_skills_are_neutral_not_missing():
    assert compare_skills(["Python"],[]) == (50.0,[],[])

def test_hybrid_ranking_breakdown_and_top_k():
    best=HybridMatcher(FakeEmbedder()).match(sample(1)).recommendations[0]
    assert best.job_id=="a" and best.match_score > 80
    assert best.matched_skills==["postgresql","python"] and best.missing_skills==["spark"]

def test_missing_role_and_empty_jobs():
    assert role_score(None,"Developer") == 50.0
    value=sample(); value.jobs=[]
    assert HybridMatcher(FakeEmbedder()).match(value).recommendations==[]

def test_french_profile_ranks_matching_technical_job():
    value=sample(1)
    value.semantic_profile="Développeuse Python spécialisée en données"
    assert HybridMatcher(FakeEmbedder()).match(value).recommendations[0].job_id=="a"

def test_multilingual_role_families_and_unrelated_roles():
    assert role_score("Data Engineering", "Ingénieur Data") == 100.0
    assert role_score("Data Engineer", "Analyste de données") == 75.0
    assert role_score("Développeur Backend", "Backend Developer") == 100.0
    assert role_score("Machine Learning Engineer", "Ingénieur Machine Learning") == 100.0
    assert role_score("DevOps Engineer", "Ingénieur Cloud") == 100.0
    assert role_score("Data Engineer", "Bioengineer") == 0.0
    assert role_score("Data Engineer", "Business Operations Intern") == 0.0
    assert role_score("Data Engineer", "Data Analist") == 75.0

def test_relevant_role_outranks_scientific_and_business_roles_with_equal_semantics():
    value=MatchRequest(semantic_profile="Data engineering profile",skills=["Python"],
      target_role="Ingénieur Data",top_k=3,jobs=[
       {"job_id":"science","title":"Doctorant plateforme multi-capteurs",
        "description":"Data monitoring Python","skills":["Python"]},
       {"job_id":"business","title":"Business Operations Intern",
        "description":"Data reporting Python","skills":["Python"]},
       {"job_id":"relevant","title":"Data Engineer","description":"Data pipelines Python",
        "skills":["Python"]}])
    result=HybridMatcher(FakeEmbedder()).match(value).recommendations
    assert [item.job_id for item in result] == ["relevant","business","science"]
    assert result[0].role_score == 100.0
    assert result[0].match_score - result[1].match_score == 20.0
    assert "different professional role family" in result[1].explanation

def test_role_explanation_matches_reported_role_score():
    best=HybridMatcher(FakeEmbedder()).match(sample(1)).recommendations[0]
    assert "Role-family compatibility is strong." in best.explanation

def test_provisional_real_title_relevance_fixture():
    fixture=Path(__file__).parent/"fixtures"/"role_relevance_cases.json"
    cases=json.loads(fixture.read_text(encoding="utf-8"))
    for case in cases:
        for job in case["jobs"]:
            score=role_score(case["targetRole"],job["title"])
            mismatch=explicit_role_mismatch(case["targetRole"],job["title"])
            if job["expected"]=="same_or_close":
                assert score>=75 and not mismatch
            elif job["expected"]=="related":
                assert score>=55 and not mismatch
            else:
                assert mismatch and score==0
