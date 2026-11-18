export interface Recommendation {
  jobId: string;
  jobTitle: string;
  company: string | null;
  location: string | null;
  source: string | null;
  jobUrl: string | null;
  matchScore: number;
  semanticScore: number;
  skillScore: number;
  roleScore: number;
  matchedSkills: string[];
  missingSkills: string[];
  explanation: string;
}

export interface RecommendationResponse {
  parserVersion: string;
  modelVersion: string;
  parsingWarnings: string[];
  recommendations: Recommendation[];
}