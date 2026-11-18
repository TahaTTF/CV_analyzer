function parse(html: string): string {
  const doc = new DOMParser().parseFromString(html, 'text/html');
  return doc.body.textContent ?? '';
}

// Converts provider text (possibly HTML, possibly double-encoded) to safe plain text.
// The result is only ever rendered with {{ }} interpolation, never innerHTML.
export function toPlainText(value: string | null | undefined): string {
  if (!value) return '';
  let text = parse(
    value.replace(/<\s*br\s*\/?>/gi, '\n').replace(/<\/(p|div|li|h[1-6])\s*>/gi, '\n'),
  );
  if (/<\/?[a-z][^>]*>/i.test(text)) {
    text = parse(text.replace(/<\s*br\s*\/?>/gi, '\n').replace(/<\/(p|div|li|h[1-6])\s*>/gi, '\n'));
  }
  return text.replace(/[ \t]+\n/g, '\n').replace(/\n{3,}/g, '\n\n').trim();
}