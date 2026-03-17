package edu.automarket.user;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class UserRepository {
    //language=postgresql
    private static final String SELECT_USER_COUNT_BY_USERNAME_QUERY = "SELECT COUNT(*) FROM users WHERE username = :username";

    //language=postgresql
    private static final String CREATE_USER_QUERY = """
            INSERT INTO users (username, phone_number, password_hash, display_name, created_at, is_active)
            VALUES (:username, :phoneNumber, :passwordHash, :displayName, :createdAt, :active)
            RETURNING id
            """;

    //language=postgresql
    private static final String SELECT_BY_USERNAME_QUERY = """
            SELECT id, username, phone_number, password_hash, display_name, created_at, is_active
            FROM users
            WHERE username = :username
            """;

    //language=postgresql
    private static final String SELECT_BY_ID_QUERY = """
            SELECT id, username, phone_number, password_hash, display_name, created_at, is_active
            FROM users
            WHERE id = :id
            """;

    private final DatabaseClient client;

    public UserRepository(DatabaseClient client) {
        this.client = client;
    }

    public Mono<Boolean> existsByUsername(String username) {
        return client.sql(SELECT_USER_COUNT_BY_USERNAME_QUERY)
                .bind("username", username)
                .map(row -> row.get(0, Long.class))
                .one()
                .map(count -> count > 0);
    }

    public Mono<User> save(User user) {
        return client.sql(CREATE_USER_QUERY)
                .bind("username", user.username())
                .bind("phoneNumber", user.phoneNumber())
                .bind("passwordHash", user.passwordHash())
                .bind("displayName", user.displayName())
                .bind("createdAt", user.createdAt())
                .bind("active", user.active())
                .map(row -> new User(
                        row.get(0, Long.class),
                        user.username(),
                        user.phoneNumber(),
                        user.passwordHash(),
                        user.displayName(),
                        user.createdAt(),
                        user.active()
                ))
                .one();
    }

    public Mono<User> findByUsername(String username) {
        return client.sql(SELECT_BY_USERNAME_QUERY)
                .bind("username", username)
                .map(row -> new User(
                        row.get(0, Long.class),
                        row.get(1, String.class),
                        row.get(2, String.class),
                        row.get(3, String.class),
                        row.get(4, String.class),
                        row.get(5, Long.class),
                        Boolean.TRUE.equals(row.get(6, Boolean.class))
                ))
                .one();
    }

    public Mono<User> findById(Long id) {
        return client.sql(SELECT_BY_ID_QUERY)
                .bind("id", id)
                .map(row -> new User(
                        row.get(0, Long.class),
                        row.get(1, String.class),
                        row.get(2, String.class),
                        row.get(3, String.class),
                        row.get(4, String.class),
                        row.get(5, Long.class),
                        Boolean.TRUE.equals(row.get(6, Boolean.class))
                ))
                .one();
    }
}
