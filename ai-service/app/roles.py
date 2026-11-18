import re
import unicodedata


ROLE_PATTERNS: dict[str, tuple[str, ...]] = {
    "data_engineering": (
        r"\bdata engineer(?:ing)?\b", r"\bingenieur data\b", r"\bingenieur donnees\b",
        r"\bمهندس بيانات\b",
    ),
    "data_analysis": (
        r"\bdata (?:analy(?:st|tics)|analist)\b", r"\banalyste (?:de )?donnees\b",
        r"\banalyste data\b", r"\bمحلل بيانات\b",
    ),
    "data_science_ml": (
        r"\bdata scien(?:tist|ce)\b", r"\bmachine learning engineer\b",
        r"\bingenieur (?:machine learning|ia)\b", r"\bml engineer\b",
        r"\bai engineer\b", r"\bمهندس تعلم آلي\b",
    ),
    "business_intelligence": (
        r"\bbusiness intelligence\b", r"\bbi (?:analyst|consultant|developer)\b",
        r"\bconsultant bi\b",
    ),
    "software_backend": (
        r"\bbackend (?:developer|engineer)\b", r"\bdeveloppeur back ?end\b",
        r"\bjava developer\b", r"\bspring (?:boot )?developer\b", r"\bمطوّر خلفي\b",
    ),
    "software_frontend": (
        r"\bfront ?end (?:developer|engineer)\b", r"\bdeveloppeur front ?end\b",
        r"\bangular developer\b", r"\breact developer\b", r"\bمطوّر واجهات\b",
    ),
    "software_fullstack": (
        r"\bfull[ -]?stack (?:developer|engineer)\b", r"\bdeveloppeur full[ -]?stack\b",
    ),
    "software_general": (
        r"\bsoftware (?:developer|engineer)\b", r"\bdeveloppeu(?:r|se) logiciel\b",
        r"\bingenieur logiciel\b",
    ),
    "devops_cloud": (
        r"\bdevops(?: engineer)?\b", r"\bcloud engineer\b", r"\bingenieur cloud\b",
        r"\bsite reliability engineer\b", r"\bsre\b", r"\bمهندس سحابة\b",
    ),
    "systems_network": (
        r"(?<!control )\bsystems? (?:administrator|engineer)\b", r"\bsystem software engineer\b",
        r"\bnetwork (?:administrator|engineer)\b", r"\badministrateur(?:rice)? (?:systemes|reseaux)\b",
        r"\bsysteembeheerder\b", r"\bمهندس شبكات\b",
    ),
    "database": (
        r"\bdatabase (?:administrator|engineer)\b", r"\bdba\b",
        r"\bingenieur bases? de donnees\b",
    ),
    "cybersecurity": (
        r"\bcyber ?security\b", r"\bsecurite informatique\b", r"\bsecurity engineer\b",
    ),
    "mobile": (
        r"\bmobile developer\b", r"\bandroid developer\b", r"\bios developer\b",
        r"\bdeveloppeur mobile\b",
    ),
    "qa_testing": (
        r"\bqa engineer\b", r"\btest automation engineer\b", r"\btesteur logiciel\b",
    ),
    "scientific_research": (
        r"\bbioengineer\b", r"\bbiomedical engineer\b", r"\bdoctorant\b",
        r"\bphd (?:student|candidate)\b", r"\blab(?:oratory)? automation\b",
        r"\bradioprotection\b", r"\binstrumentation engineer\b",
        r"\bresearch associate\b", r"\bmolecular biology\b", r"\bscientist\b",
    ),
    "business_operations": (
        r"\bbusiness operations\b", r"\bsales manager\b", r"\baccount manager\b",
        r"\bgestion d entreprise\b", r"\bconseiller multimarques\b", r"\bfounders associate\b",
    ),
    "industrial_engineering": (
        r"\bcontrol systems engineer\b", r"\belectrical engineer\b",
        r"\bingenieur electricite\b", r"\bcharge d etudes en electricite\b",
    ),
}

RELATED: dict[str, dict[str, float]] = {
    "data_engineering": {
        "data_analysis": 75.0, "data_science_ml": 70.0, "business_intelligence": 75.0,
        "database": 80.0, "devops_cloud": 65.0, "software_backend": 55.0,
    },
    "data_analysis": {
        "data_engineering": 75.0, "data_science_ml": 75.0, "business_intelligence": 90.0,
    },
    "data_science_ml": {
        "data_engineering": 70.0, "data_analysis": 75.0, "business_intelligence": 65.0,
    },
    "software_backend": {
        "software_frontend": 55.0, "software_fullstack": 90.0, "software_general": 85.0,
        "devops_cloud": 65.0,
        "database": 65.0, "data_engineering": 55.0,
    },
    "software_frontend": {"software_backend": 55.0, "software_fullstack": 90.0, "mobile": 60.0},
    "software_fullstack": {"software_backend": 90.0, "software_frontend": 90.0,
                           "software_general": 85.0, "devops_cloud": 55.0},
    "software_general": {"software_backend": 85.0, "software_frontend": 75.0,
                         "software_fullstack": 85.0, "devops_cloud": 55.0},
    "devops_cloud": {"systems_network": 80.0, "software_backend": 65.0,
                     "software_general": 55.0, "data_engineering": 65.0},
    "systems_network": {"devops_cloud": 80.0, "cybersecurity": 65.0},
    "database": {"data_engineering": 80.0, "software_backend": 65.0},
    "business_intelligence": {"data_analysis": 90.0, "data_engineering": 75.0, "data_science_ml": 65.0},
}

IT_ROLE_FAMILIES = {
    "data_engineering", "data_analysis", "data_science_ml", "business_intelligence",
    "software_backend", "software_frontend", "software_fullstack", "software_general",
    "devops_cloud", "systems_network", "database", "cybersecurity", "mobile", "qa_testing",
}
EXPLICIT_NON_IT_FAMILIES = {"scientific_research", "business_operations", "industrial_engineering"}

GENERIC_ROLE_TOKENS = {
    "engineer", "engineering", "ingenieur", "developer", "developpeur", "consultant",
    "junior", "senior", "intern", "internship", "stage", "alternance", "h", "f", "m", "x",
}


def normalize_role(value: str) -> str:
    text = unicodedata.normalize("NFKD", value or "")
    text = "".join(character for character in text if not unicodedata.combining(character)).lower()
    text = re.sub(r"[^\w+#]+", " ", text, flags=re.UNICODE)
    return re.sub(r"\s+", " ", text).strip()


def role_families(value: str) -> set[str]:
    normalized = normalize_role(value)
    return {
        family for family, patterns in ROLE_PATTERNS.items()
        if any(re.search(pattern, normalized, flags=re.UNICODE) for pattern in patterns)
    }


def role_score(target: str | None, title: str) -> float:
    if not target:
        return 50.0
    normalized_target = normalize_role(target)
    normalized_title = normalize_role(title)
    target_families = role_families(normalized_target)
    title_families = role_families(normalized_title)
    if target_families and title_families:
        if target_families & title_families:
            return 100.0
        related = [RELATED.get(expected, {}).get(actual, 0.0)
                   for expected in target_families for actual in title_families]
        return max(related, default=0.0)

    expected = set(normalized_target.split()) - GENERIC_ROLE_TOKENS
    actual = set(normalized_title.split()) - GENERIC_ROLE_TOKENS
    if not expected:
        return 50.0
    overlap = 100.0 * len(expected & actual) / len(expected)
    if target_families and not title_families:
        return min(50.0, overlap)
    return overlap


def explicit_role_mismatch(target: str | None, title: str) -> bool:
    if not target:
        return False
    expected = role_families(target)
    actual = role_families(title)
    if not (expected & IT_ROLE_FAMILIES) or not (actual & EXPLICIT_NON_IT_FAMILIES):
        return False
    if actual & IT_ROLE_FAMILIES:
        return False
    return all(RELATED.get(left, {}).get(right, 0.0) == 0.0
               for left in expected for right in actual)
