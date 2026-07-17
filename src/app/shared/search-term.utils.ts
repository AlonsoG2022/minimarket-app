export function normalizeLookupTerm(value: string | null | undefined): string {
  return value?.trim().toLowerCase() ?? '';
}

export function looksLikeProductCode(value: string | null | undefined): boolean {
  const normalized = value?.trim() ?? '';
  if (!normalized || /\s/.test(normalized)) {
    return false;
  }

  if (!/^[A-Za-z0-9._-]+$/.test(normalized)) {
    return false;
  }

  return /\d/.test(normalized) || /^[A-Z0-9._-]+$/.test(normalized);
}
