package edu.kai.automarket.user;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class UserRepository {
    //language=postgresql
    private static final String SELECT_USER_COUNT_BY_EMAIL_QUERY = "SELECT COUNT(*) FROM users WHERE email = :email";

    //language=postgresql
    private static final String CREATE_USER_QUERY = """
            INSERT INTO users (email, password_hash, display_name, created_at, is_active)
            VALUES (:email, :passwordHash, :displayName, :createdAt, :active)
            RETURNING id
            """;

    //language=postgresql
    private static final String SELECT_BY_EMAIL_QUERY = """
            SELECT id, email, password_hash, display_name, created_at, is_active
            FROM users
            WHERE email = :email
            """;

    //language=postgresql
    private static final String SELECT_BY_ID_QUERY = """
            SELECT id, email, password_hash, display_name, created_at, is_active
            FROM users
            WHERE id = :id
            """;

    private final DatabaseClient client;

    public UserRepository(DatabaseClient client) {
        this.client = client;
    }

    public Mono<Boolean> existsByEmail(String email) {
        return client.sql(SELECT_USER_COUNT_BY_EMAIL_QUERY)
                .bind("email", email)
                .map(row -> row.get(0, Long.class))
                .one()
                .map(count -> count > 0);
    }

    public Mono<User> save(User user) {
        return client.sql(CREATE_USER_QUERY)
                .bind("email", user.email())
                .bind("passwordHash", user.passwordHash())
                .bind("displayName", user.displayName())
                .bind("createdAt", user.createdAt())
                .bind("active", user.active())
                .map(row -> new User(
                        row.get(0, Long.class),
                        user.email(),
                        user.passwordHash(),
                        user.displayName(),
                        user.createdAt(),
                        user.active()
                ))
                .one();
    }

    public Mono<User> findByEmail(String email) {
        return client.sql(SELECT_BY_EMAIL_QUERY)
                .bind("email", email)
                .map(row -> new User(
                        row.get(0, Long.class),
                        row.get(1, String.class),
                        row.get(2, String.class),
                        row.get(3, String.class),
                        row.get(4, Long.class),
                        Boolean.TRUE.equals(row.get(5, Boolean.class))
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
                        row.get(4, Long.class),
                        Boolean.TRUE.equals(row.get(5, Boolean.class))
                ))
                .one();
    }
}
