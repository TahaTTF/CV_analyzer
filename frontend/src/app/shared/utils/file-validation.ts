export const MAX_CV_BYTES = 5 * 1024 * 1024; // 5 MiB, same limit as the backend

export function validateCvFile(file: File): string | null {
  const name = file.name.toLowerCase();
  if (!name.endsWith('.pdf') && !name.endsWith('.docx')) {
    return 'Please choose a PDF or DOCX file.';
  }
  if (file.size === 0) {
    return 'This file is empty.';
  }
  if (file.size > MAX_CV_BYTES) {
    return 'This file is larger than 5 MB.';
  }
  return null;
}

export function isSafeHttpUrl(value: string | null | undefined): boolean {
  if (!value) return false;
  try {
    const url = new URL(value);
    return url.protocol === 'http:' || url.protocol === 'https:';
  } catch {
    return false;
  }
}