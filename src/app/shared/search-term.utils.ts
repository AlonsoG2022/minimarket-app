export function normalizeLookupTerm(value: string | null | undefined): string {
  return (value ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .trim()
    .toLowerCase();
}

export function matchesProductLookup(query: string | null | undefined, values: Array<string | null | undefined>): boolean {
  const terms = normalizeLookupTerm(query)
    .replace(/[^a-z0-9]+/g, ' ')
    .split(' ')
    .filter(Boolean);

  if (!terms.length) {
    return true;
  }

  const searchableText = values
    .map((value) => normalizeLookupTerm(value).replace(/[^a-z0-9]+/g, ' '))
    .join(' ');

  return terms.every((term) => searchableText.includes(term));
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
