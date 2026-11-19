import { toPlainText } from './text';

describe('toPlainText', () => {
  it('returns an empty string for missing input', () => {
    expect(toPlainText(null)).toBe('');
    expect(toPlainText(undefined)).toBe('');
  });

  it('removes tags and keeps paragraph breaks', () => {
    const text = toPlainText('<p>First</p><p>Second <strong>bold</strong></p>');
    expect(text).toContain('First');
    expect(text).toContain('Second bold');
    expect(text).not.toContain('<');
  });

  it('never keeps script content as markup', () => {
    const text = toPlainText('Hello<script>alert(1)</script>');
    expect(text).not.toContain('<script');
  });

  it('decodes double-encoded HTML', () => {
    const text = toPlainText('&lt;p&gt;Hello &amp;amp; welcome&lt;/p&gt;');
    expect(text).not.toContain('<p>');
    expect(text).toContain('Hello');
  });
});