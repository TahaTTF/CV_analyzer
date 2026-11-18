import { ApiError } from '../models/api-error.model';

const MESSAGES: Record<string, string> = {
  CV_FILE_REQUIRED: 'Please choose a file to upload.',
  CV_FILE_EMPTY: 'This file is empty. Please choose another one.',
  CV_FORMAT_UNSUPPORTED: 'Only PDF and DOCX files are supported.',
  CV_DOCX_MACROS_NOT_ALLOWED: 'Word files with macros are not allowed. Save your CV as a regular .docx and try again.',
  CV_PDF_ENCRYPTED: 'This PDF is password-protected. Please upload an unencrypted version.',
  CV_PDF_NO_TEXT: 'No selectable text was found. Scanned or image-only PDFs are not supported (there is no OCR yet).',
  CV_PDF_CORRUPTED: 'This PDF appears to be corrupted. Please export it again.',
  CV_DOCX_NO_TEXT: 'No text was found in this Word file.',
  CV_DOCX_CORRUPTED: 'This Word file appears to be corrupted. Please save it again.',
  CV_FILE_TOO_LARGE: 'This file is larger than the 5 MB limit.',
  CV_PDF_TOO_MANY_PAGES: 'This PDF has more than the 100 pages allowed.',
  CV_TEXT_TOO_LARGE: 'The text in this file is too long to process.',
  NETWORK_UNAVAILABLE: 'Cannot reach the server. Check that the backend is running and try again.',
  NO_JOBS_AVAILABLE: 'There are no jobs available to match against yet.',
  AI_SERVICE_UNAVAILABLE: 'The matching service is unavailable. Your CV is still loaded, so you can retry.',
  AI_SERVICE_TIMEOUT: 'The matching service took too long. The first request can be slow while the model starts, so try once more.',
  AI_SERVICE_FAILURE: 'The matching service failed. Please try again later.',
  AI_RESPONSE_INVALID: 'The matching service returned an invalid answer. Please try again later.',
  DATABASE_UNAVAILABLE: 'Job data is temporarily unavailable. Please retry in a moment.',
  RESOURCE_NOT_FOUND: 'We could not find what you were looking for.',
  VALIDATION_ERROR: 'Some of the values you entered are not valid.',
};

export function friendlyMessage(error: ApiError): string {
  return MESSAGES[error.code] ?? error.message ?? 'Something went wrong.';
}