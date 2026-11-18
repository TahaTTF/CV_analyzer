import re
import unicodedata

ALIASES = {
    "js": "javascript", "javascript": "javascript", "py": "python", "python": "python",
    "postgres": "postgresql", "postgresql": "postgresql",
    "node": "node.js", "nodejs": "node.js", "node.js": "node.js",
    "reactjs": "react", "react.js": "react", "react": "react",
    "k8s": "kubernetes", "kubernetes": "kubernetes",
    "gcp": "google cloud", "google cloud": "google cloud",
    "amazon web services": "aws", "aws": "aws",
    "microsoft azure": "azure", "azure": "azure",
    "rest api": "rest apis", "restful api": "rest apis", "rest apis": "rest apis",
    "springboot": "spring boot", "spring boot": "spring boot",
    "powerbi": "power bi", "power bi": "power bi",
    "sklearn": "scikit-learn", "scikit learn": "scikit-learn", "scikit-learn": "scikit-learn",
    "mssql": "sql server", "microsoft sql server": "sql server", "sql server": "sql server",
    "dotnet": ".net", ".net": ".net",
    "c sharp": "c#", "c#": "c#", "c plus plus": "c++", "c++": "c++",
}

def normalize_skill(value: str) -> str:
    text = unicodedata.normalize("NFKD", value).encode("ascii", "ignore").decode().lower().strip()
    text = re.sub(r"[^a-z0-9+#.]+", " ", text)
    text = re.sub(r"\s+", " ", text).strip().rstrip(".")
    return ALIASES.get(text, text)

def normalize_skills(values: list[str]) -> list[str]:
    return sorted({normalized for value in values if (normalized := normalize_skill(value))})

def compare_skills(candidate: list[str], required: list[str]) -> tuple[float, list[str], list[str]]:
    cv, job = set(normalize_skills(candidate)), set(normalize_skills(required))
    if not job:
        return 50.0, [], []
    matched, missing = sorted(cv & job), sorted(job - cv)
    return 100.0 * len(matched) / len(job), matched, missing
