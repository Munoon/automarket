package edu.automarket.common.validation;

public enum CharacterType {
    ALPHABETICAL {
        @Override
        public boolean matches(char c) { return Character.isLetter(c); }
    },
    DIGIT {
        @Override
        public boolean matches(char c) { return Character.isDigit(c); }
    },
    SPECIAL_SYMBOL {
        @Override
        public boolean matches(char c) {
            return c == ' ' || c == '_' || c == '-' || c == '\'' || c == '"'
                    || c == '.' || c == ',' || c == '!' || c == '?' || c == '@'
                    || c == '+' || c == '(' || c == ')' || c == ':' || c == ';'
                    || c == '#' || c == '$' || c == '%' || c == '&' || c == '*'
                    || c == '/';
        }
    };

    public abstract boolean matches(char c);
}
