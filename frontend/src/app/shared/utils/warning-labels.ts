const LABELS: Record<string, string> = {
  WORK_EXPERIENCE_NOT_FOUND: 'No work experience section was reliably detected.',
};

// Unknown codes get a readable fallback, e.g. EDUCATION_NOT_FOUND -> "Education not found".
export function warningLabel(code: string): string {
  if (LABELS[code]) return LABELS[code];
  const words = code.toLowerCase().replace(/_/g, ' ');
  return words.charAt(0).toUpperCase() + words.slice(1);
}