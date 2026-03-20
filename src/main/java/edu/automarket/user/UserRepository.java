package edu.automarket.user;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class UserRepository {
    //language=postgresql
    private static final String CREATE_USER_QUERY = """
            INSERT INTO users (phone_number, created_at, is_active)
            VALUES (:phoneNumber, :createdAt, true)
            RETURNING id
            """;

    //language=postgresql
    private static final String SELECT_BY_ID_QUERY = """
            SELECT id, phone_number, display_name, created_at, is_active
            FROM users
            WHERE id = :id
            """;

    //language=postgresql
    private static final String SELECT_BY_PHONE_NUMBER_QUERY = """
            SELECT id, phone_number, display_name, created_at, is_active
            FROM users
            WHERE phone_number = :phoneNumber
            """;

    //language=postgresql
    private static final String UPDATE_DISPLAY_NAME_BY_ID = """
            UPDATE users
            SET display_name = :displayName
            WHERE id = :id
            """;

    private final DatabaseClient client;

    public UserRepository(DatabaseClient client) {
        this.client = client;
    }

    public Mono<User> register(String phoneNumber) {
        long createdAt = System.currentTimeMillis();
        return client.sql(CREATE_USER_QUERY)
                     .bind("phoneNumber", phoneNumber)
                     .bind("createdAt", createdAt)
                     .map(row -> new User(
                        row.get(0, Long.class),
                        phoneNumber,
                        null,
                        createdAt,
                        true
                     ))
                     .one();
    }

    public Mono<User> findById(long id) {
        return client.sql(SELECT_BY_ID_QUERY)
                .bind("id", id)
                .map(row -> new User(
                        row.get(0, Long.class),
                        row.get(1, String.class),
                        row.get(2, String.class),
                        row.get(3, Long.class),
                        Boolean.TRUE.equals(row.get(4, Boolean.class))
                ))
                .one();
    }

    public Mono<User> findByPhoneNumber(String phoneNumber) {
        return client.sql(SELECT_BY_PHONE_NUMBER_QUERY)
                .bind("phoneNumber", phoneNumber)
                .map(row -> new User(
                        row.get(0, Long.class),
                        row.get(1, String.class),
                        row.get(2, String.class),
                        row.get(3, Long.class),
                        Boolean.TRUE.equals(row.get(4, Boolean.class))
                ))
                .one();
    }

    public Mono<Void> updateDisplayName(long userId, String displayName) {
        return client.sql(UPDATE_DISPLAY_NAME_BY_ID)
                .bind("id", userId)
                .bind("displayName", displayName)
                .fetch().rowsUpdated().then();
    }
}
