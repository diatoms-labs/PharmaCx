/** Convert ENUM_CASE to Title Case (e.g. "QUALITY_ASSURANCE" → "Quality Assurance") */
export const fmt = (s: string) => s.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
