package edu.automarket.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AllowedCharactersValidator implements ConstraintValidator<AllowedCharacters, String> {
    private CharacterType[] allowedTypes;

    @Override
    public void initialize(AllowedCharacters constraint) {
        this.allowedTypes = constraint.value();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            boolean allowed = false;
            for (CharacterType type : allowedTypes) {
                if (type.matches(c)) {
                    allowed = true;
                    break;
                }
            }

            if (!allowed) {
                return false;
            }
        }

        return true;
    }
}
