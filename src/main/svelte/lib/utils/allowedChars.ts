function isLetter(c: string): boolean {
  return /\p{L}/u.test(c);
}

function isDigit(c: string): boolean {
  return c >= '0' && c <= '9';
}

export type CharacterType = 'ALPHABETICAL' | 'DIGIT' | 'SPACE' | 'UNDERSCORE' | 'HYPHEN' | 'APOSTROPHE';

function matchesType(c: string, type: CharacterType): boolean {
  switch (type) {
    case 'ALPHABETICAL': return isLetter(c);
    case 'DIGIT': return isDigit(c);
    case 'SPACE': return c === ' ';
    case 'UNDERSCORE': return c === '_';
    case 'HYPHEN': return c === '-';
    case 'APOSTROPHE': return c === "'";
  }
}

export function allowedChars(value: string, types: CharacterType[]): boolean {
  for (const c of value) {
    if (!types.some(type => matchesType(c, type))) return false;
  }
  return true;
}
