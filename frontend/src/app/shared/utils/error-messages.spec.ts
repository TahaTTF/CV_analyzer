import { ApiError } from '../models/api-error.model';
import { friendlyMessage } from './error-messages';
import { warningLabel } from './warning-labels';

const base: ApiError = {
  timestamp: '', status: 400, code: '', message: 'raw message',
  path: '/x', fieldErrors: [], traceId: null,
};

describe('friendlyMessage', () => {
  it('maps known backend codes', () => {
    expect(friendlyMessage({ ...base, code: 'CV_PDF_NO_TEXT' })).toContain('OCR');
    expect(friendlyMessage({ ...base, code: 'NO_JOBS_AVAILABLE' })).toContain('no jobs');
  });

  it('falls back to the server message for unknown codes', () => {
    expect(friendlyMessage({ ...base, code: 'SOMETHING_NEW' })).toBe('raw message');
  });
});

describe('warningLabel', () => {
  it('uses a readable fallback for unknown codes', () => {
    expect(warningLabel('EDUCATION_NOT_FOUND')).toBe('Education not found');
  });

  it('uses a specific label for known codes', () => {
    expect(warningLabel('WORK_EXPERIENCE_NOT_FOUND')).toContain('work experience');
  });
});