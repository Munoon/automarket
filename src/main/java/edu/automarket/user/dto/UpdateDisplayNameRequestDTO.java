package edu.automarket.user.dto;

import edu.automarket.common.validation.AllowedCharacters;
import edu.automarket.common.validation.CharacterType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDisplayNameRequestDTO(
        @NotBlank
        @Size(max = 100)
        @AllowedCharacters({ CharacterType.ALPHABETICAL, CharacterType.SPACE, CharacterType.APOSTROPHE})
        String displayName
) {
}
