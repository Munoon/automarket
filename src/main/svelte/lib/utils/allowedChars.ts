function isLetter(c: string): boolean {
  return /\p{L}/u.test(c);
}

function isDigit(c: string): boolean {
  return c >= '0' && c <= '9';
}

export type CharacterType = 'ALPHABETICAL' | 'DIGIT' | 'SPECIAL_SYMBOL';

function matchesType(c: string, type: CharacterType): boolean {
  switch (type) {
    case 'ALPHABETICAL': return isLetter(c);
    case 'DIGIT': return isDigit(c);
    case 'SPECIAL_SYMBOL': return c === ' ' || c === '_' || c === '-' || c === '\'' || c === '"'
                    || c === '.' || c === ',' || c === '!' || c === '?' || c === '@'
                    || c === '+' || c === '(' || c === ')' || c === ':' || c === ';'
                    || c === '#' || c === '$' || c === '%' || c === '&' || c === '*'
                    || c === '/';
  }
}

export function allowedChars(value: string, types: CharacterType[]): boolean {
  for (const c of value) {
    if (!types.some(type => matchesType(c, type))) return false;
  }
  return true;
}
