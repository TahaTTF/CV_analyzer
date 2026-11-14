from pydantic import BaseModel, ConfigDict, Field

def to_camel(value: str) -> str:
    first, *rest = value.split("_")
    return first + "".join(part.title() for part in rest)

class ApiModel(BaseModel):
    model_config = ConfigDict(extra="forbid", alias_generator=to_camel, populate_by_name=True)

class JobCandidate(ApiModel):
    job_id: str = Field(min_length=1)
    title: str = Field(min_length=1)
    description: str = ""
    skills: list[str] = Field(default_factory=list)
    categories: list[str] = Field(default_factory=list)

class MatchRequest(ApiModel):
    semantic_profile: str = Field(min_length=1, max_length=50_000)
    skills: list[str] = Field(default_factory=list)
    target_role: str | None = Field(default=None, max_length=500)
    jobs: list[JobCandidate] = Field(max_length=2_000)
    top_k: int = Field(default=10, ge=1, le=50)

class MatchResult(ApiModel):
    job_id: str
    match_score: float
    semantic_score: float
    skill_score: float
    role_score: float
    matched_skills: list[str]
    missing_skills: list[str]
    explanation: str

class MatchResponse(ApiModel):
    model_version: str
    recommendations: list[MatchResult]
