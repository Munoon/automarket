package edu.automarket.user.dto;

import edu.automarket.common.validation.AllowedCharacters;
import edu.automarket.common.validation.CharacterType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequestDTO(
        @NotBlank
        @Size(min = 3, max = 50)
        @AllowedCharacters({CharacterType.ALPHABETICAL, CharacterType.DIGIT, CharacterType.UNDERSCORE, CharacterType.HYPHEN})
        String username,

        @Size(min = 1, max = 255)
        String passwordHash
) {
}
