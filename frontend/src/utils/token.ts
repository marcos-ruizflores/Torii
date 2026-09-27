/**
 * Resolved value of a CSS custom property on <html>. SVG presentation attributes
 * (fill, stroke, stopColor) don't accept var(), so charts and maps read the theme
 * tokens through this instead.
 */
export function token(name: string): string {
  if (typeof window === 'undefined') return ''
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}
