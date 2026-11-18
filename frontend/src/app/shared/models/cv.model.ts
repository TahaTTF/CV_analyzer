export interface ExperienceEntry {
  role: string | null;
  organization: string | null;
  period: string | null;
  description: string | null;
}

export interface ProjectEntry {
  name: string | null;
  description: string | null;
  technologies: string[];
}

export interface EducationEntry {
  qualification: string | null;
  institution: string | null;
  period: string | null;
  description: string | null;
}

export interface CandidateProfile {
  targetRole: string | null;
  professionalSummary: string | null;
  technicalSkills: string[];
  yearsOfExperience: number | null;
  experience: ExperienceEntry[];
  projects: ProjectEntry[];
  education: EducationEntry[];
  languages: string[];
}

export interface CvParseResponse {
  parsingStatus: 'SUCCESS';
  parserVersion: string;
  profile: CandidateProfile;
  professionalSummaryText: string | null;
  skillsText: string | null;
  experienceText: string | null;
  projectsText: string | null;
  educationText: string | null;
  semanticProfileText: string | null;
  warnings: string[];
}