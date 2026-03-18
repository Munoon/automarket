package edu.automarket;

import edu.automarket.user.dto.RegisterRequestDTO;

public class TestUtils {
    public static RegisterRequestDTO testUser(String username) {
        return new RegisterRequestDTO(username, "+123456789012", "hash", "Test User");
    }
}
