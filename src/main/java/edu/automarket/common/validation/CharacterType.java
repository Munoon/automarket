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
    SPACE {
        @Override
        public boolean matches(char c) { return c == ' '; }
    },
    UNDERSCORE {
        @Override
        public boolean matches(char c) { return c == '_'; }
    },
    HYPHEN {
        @Override
        public boolean matches(char c) { return c == '-'; }
    },
    APOSTROPHE {
        @Override
        public boolean matches(char c) { return c == '\''; }
    };

    public abstract boolean matches(char c);
}
