package edu.automarket.user.dto;

import edu.automarket.common.validation.AllowedCharacters;
import edu.automarket.common.validation.CharacterType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(
        @NotBlank
        @Size(min = 3, max = 50)
        @AllowedCharacters({CharacterType.ALPHABETICAL, CharacterType.DIGIT, CharacterType.UNDERSCORE, CharacterType.HYPHEN})
        String username,

        @Pattern(regexp = "^\\+\\d{12}$", message = "Phone number must be in format +XXXXXXXXXXXX")
        String phoneNumber,

        @Size(min = 1, max = 255)
        String passwordHash,

        @NotBlank
        @Size(max = 100)
        @AllowedCharacters({CharacterType.ALPHABETICAL, CharacterType.SPACE, CharacterType.APOSTROPHE})
        String displayName
) {
}
