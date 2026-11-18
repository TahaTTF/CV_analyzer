const SOURCES: Record<string, string> = {
  ARBEITNOW: 'Arbeitnow',
  FRANCE_TRAVAIL: 'France Travail',
  ADZUNA: 'Adzuna',
  LA_BONNE_ALTERNANCE: 'La bonne alternance',
};

const WORK_MODES: Record<string, string> = {
  REMOTE: 'Remote',
  HYBRID: 'Hybrid',
  ON_SITE: 'On-site',
};

export function sourceLabel(source: string | null | undefined): string {
  return source ? (SOURCES[source] ?? source) : '';
}

// UNKNOWN or missing work modes return an empty string so the UI can hide them.
export function workModeLabel(mode: string | null | undefined): string {
  return mode ? (WORK_MODES[mode] ?? '') : '';
}

export function humanize(code: string): string {
  const words = code.toLowerCase().replace(/_/g, ' ');
  return words.charAt(0).toUpperCase() + words.slice(1);
}