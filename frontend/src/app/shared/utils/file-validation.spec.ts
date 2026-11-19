import { MAX_CV_BYTES, isSafeHttpUrl, validateCvFile } from './file-validation';

describe('validateCvFile', () => {
  it('accepts a PDF and a DOCX', () => {
    expect(validateCvFile(new File(['x'], 'cv.pdf'))).toBeNull();
    expect(validateCvFile(new File(['x'], 'CV.DOCX'))).toBeNull();
  });

  it('rejects other extensions', () => {
    expect(validateCvFile(new File(['x'], 'cv.txt'))).toContain('PDF or DOCX');
  });

  it('rejects an empty file', () => {
    expect(validateCvFile(new File([], 'cv.pdf'))).toContain('empty');
  });

  it('rejects a file larger than 5 MB', () => {
    const big = new File([new Uint8Array(MAX_CV_BYTES + 1)], 'cv.pdf');
    expect(validateCvFile(big)).toContain('5 MB');
  });
});

describe('isSafeHttpUrl', () => {
  it('accepts http and https', () => {
    expect(isSafeHttpUrl('https://example.test/job/1')).toBe(true);
    expect(isSafeHttpUrl('http://example.test')).toBe(true);
  });

  it('rejects unsafe or invalid values', () => {
    expect(isSafeHttpUrl('javascript:alert(1)')).toBe(false);
    expect(isSafeHttpUrl('ftp://example.test')).toBe(false);
    expect(isSafeHttpUrl('not a url')).toBe(false);
    expect(isSafeHttpUrl(null)).toBe(false);
  });
});
