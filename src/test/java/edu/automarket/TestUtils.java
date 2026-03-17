package edu.automarket;

import edu.automarket.user.User;

public class TestUtils {
    public static User testUser(String username) {
        return new User(null, username, "+123456789012", "hash", "Test User", System.currentTimeMillis(), true);
    }
}
