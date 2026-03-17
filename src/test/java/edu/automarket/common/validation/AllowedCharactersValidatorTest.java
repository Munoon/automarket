package edu.automarket.common.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AllowedCharactersValidatorTest {

    private static Validator validator;

    private record AlphanumericOnly(
            @AllowedCharacters({CharacterType.ALPHABETICAL, CharacterType.DIGIT})
            String value
    ) {}

    private record AllTypesAllowed(
            @AllowedCharacters({CharacterType.ALPHABETICAL, CharacterType.DIGIT, CharacterType.SPACE,
                    CharacterType.UNDERSCORE, CharacterType.HYPHEN, CharacterType.APOSTROPHE})
            String value
    ) {}

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void nullValuePassesValidation() {
        assertThat(validate(new AlphanumericOnly(null))).isEmpty();
    }

    @Test
    void emptyStringPassesValidation() {
        assertThat(validate(new AlphanumericOnly(""))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "ABC", "abc123", "Z99"})
    void alphabeticalAndDigitsAreAllowed(String value) {
        assertThat(validate(new AlphanumericOnly(value))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "_", "-", "'", "abc!", "hello world", "test@value"})
    void disallowedCharsFailValidation(String value) {
        assertThat(validate(new AlphanumericOnly(value))).hasSize(1);
    }

    @Test
    void alphabeticalMatchesUnicodeLetters() {
        assertThat(validate(new AlphanumericOnly("café"))).isEmpty();
    }

    @Test
    void allTypesAllowAllSixCharacterCategories() {
        assertThat(validate(new AllTypesAllowed("Hello World_-'123"))).isEmpty();
    }

    @Test
    void eachCharacterTypeMatchesOnlyItsCharacters() {
        assertThat(CharacterType.ALPHABETICAL.matches('a')).isTrue();
        assertThat(CharacterType.ALPHABETICAL.matches('Z')).isTrue();
        assertThat(CharacterType.ALPHABETICAL.matches('1')).isFalse();

        assertThat(CharacterType.DIGIT.matches('0')).isTrue();
        assertThat(CharacterType.DIGIT.matches('9')).isTrue();
        assertThat(CharacterType.DIGIT.matches('a')).isFalse();

        assertThat(CharacterType.SPACE.matches(' ')).isTrue();
        assertThat(CharacterType.SPACE.matches('\t')).isFalse();

        assertThat(CharacterType.UNDERSCORE.matches('_')).isTrue();
        assertThat(CharacterType.UNDERSCORE.matches('-')).isFalse();

        assertThat(CharacterType.HYPHEN.matches('-')).isTrue();
        assertThat(CharacterType.HYPHEN.matches('_')).isFalse();

        assertThat(CharacterType.APOSTROPHE.matches('\'')).isTrue();
        assertThat(CharacterType.APOSTROPHE.matches('"')).isFalse();
    }

    private <T> Set<ConstraintViolation<T>> validate(T object) {
        return validator.validate(object);
    }
}
