package edu.automarket.user.dto;

public record ProfileResponseDTO(
        UserDTO user,
        LimitsDTO limits,
        long ownListingsCount
) {
}
