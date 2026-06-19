// Genera un nombre corto (para el ticket) a partir del nombre largo de un producto.
// Replica la logica del backend (ShortNameGenerator) para mostrar la sugerencia en vivo
// mientras se crea/edita un producto. Solo es un respaldo: el usuario puede editarlo.

const MAX_LENGTH = 60;
const TARGET_LENGTH = 26;

const SIZE_RE = /(\d+([.,]\d+)?\s?(ml|lt|l|kg|g|cm|mm|m)\b)|(\d+\s?(uni|unidades|rollos?|latas?|hojas?|packs?))/gi;

const FILLER = [
  'sabor', 'en polvo', 'pet', 'retornable', 'aromatizado', 'aromatizada', 'premium', 'gourmet',
  'instantanea', 'instantaneo', 'con vitamina c', 'fragancia', 'frangancia', 'de mesa', 'del valle',
  'original', 'spray', 'aerosol', 'liquido', 'complete', 'intense', 'pro-v', 'pro v', 'sin sal',
  'doble hoja', 'mayor ahorro', 'mayor resistencia', 'aroma a', 'aroma', 'multiuso', 'clasica', 'clasico'
];

const ABBR: ReadonlyArray<readonly [string, string]> = [
  ['head & shoulders', 'H&S'], ['head shoulder', 'H&S'], ['johnnie walker', 'JW'],
  ['faber-castell', 'Faber'], ['faber castell', 'Faber'], ['antitranspirante', 'Antitr'],
  ['acondicionador', 'Acond'], ['desodorante', 'Desod'], ['detergente', 'Deterg'],
  ['papel higienico', 'PH'], ['barra de chocolate', 'Choc'], ['tableta de chocolate', 'Choc'],
  ['gaseosa', ''], ['lavavajilla', 'Lavavaj'], ['ambientador', 'Ambient'], ['prestobarba', 'Prestob'],
  ['pasta dental', 'P.Dental'], ['cepillo dental', 'Cep.Dental'], ['servilletas', 'Servill'],
  ['limpiatodo', 'Limpiat'], ['galletas', 'Gall'], ['galleta', 'Gall'], ['shampoo', 'Sh'], ['yogurt', 'Yog']
];

const stripAccents = (s: string) => s.normalize('NFD').replace(/[̀-ͯ]/g, '');
const escapeRegex = (s: string) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

export function generateShortName(name: string): string {
  if (!name || !name.trim()) {
    return '';
  }

  const clean = stripAccents(name.trim());
  const matches = clean.match(SIZE_RE);
  const size = matches
    ? matches[matches.length - 1].replace(/\s+/g, '').replace(/unidades/i, 'uni')
    : '';

  let s = clean.replace(SIZE_RE, ' ');
  for (const [from, to] of ABBR) {
    s = s.replace(new RegExp(escapeRegex(from), 'gi'), to);
  }
  for (const word of FILLER) {
    s = s.replace(new RegExp('\\b' + escapeRegex(word) + '\\b', 'gi'), ' ');
  }
  s = s.replace(/\s+/g, ' ').trim();

  const limit = TARGET_LENGTH - (size ? size.length + 1 : 0);
  if (s.length > limit) {
    const words = s.split(' ').filter(Boolean);
    if (words.length > 1) {
      const first = words[0];
      const last = words[words.length - 1];
      let acc = first;
      for (const x of words.slice(1, -1)) {
        if ((acc + ' ' + x + ' ' + last).length <= limit) {
          acc = acc + ' ' + x;
        }
      }
      const candidate = (acc + ' ' + last).trim();
      s = candidate.length <= limit ? candidate : first + ' ' + last;
    } else {
      s = s.slice(0, limit);
    }
  }

  const result = (s + (size ? ' ' + size : '')).trim();
  return result.length > MAX_LENGTH ? result.slice(0, MAX_LENGTH) : result;
}
