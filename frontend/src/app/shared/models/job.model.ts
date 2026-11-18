export type JobSource = 'ARBEITNOW' | 'FRANCE_TRAVAIL' | 'ADZUNA' | 'LA_BONNE_ALTERNANCE';
export type JobRelevance = 'DEFAULT' | 'ALL' | 'IT_ONLY' | 'NON_IT' | 'UNCLASSIFIED' | 'AMBIGUOUS';
export type WorkMode = 'REMOTE' | 'HYBRID' | 'ON_SITE' | 'UNKNOWN';

export interface JobSummary {
  id: string;
  title: string;
  company: string | null;
  location: string | null;
  remote: boolean;
  source: JobSource;
  skills: string[];
  url: string | null;
  isItRelevant: boolean | null;
  matchedCategories: string[];
  classificationVersion: string | null;
}

export interface JobPage {
  items: JobSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface SkillEvidence {
  name: string;
  evidenceSources: string[];
}

export interface NormalizedLocation {
  original: string | null;
  city: string | null;
  region: string | null;
  countryCode: string | null;
  workMode: WorkMode | null;
  confidence: number | null;
}

export interface JobDetails extends JobSummary {
  description: string | null;
  requirements: { skills: string[] };
  employmentTypes: string[];
  publishedAt: string | null;
  relevanceReason: string | null;
  descriptionText: string | null;
  extractedSkills: SkillEvidence[];
  toBeAcquiredSkills: SkillEvidence[];
  normalizedLocation: NormalizedLocation | null;
  enrichmentVersion: string | null;
}

export interface JobFilters {
  page: number;
  size: number;
  query?: string;
  location?: string;
  source?: JobSource | '';
  relevance?: JobRelevance;
}