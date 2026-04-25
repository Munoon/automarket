package edu.automarket.upload;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public class UploadRepository {
    //language=postgresql
    private static final String SAVE_USER_UPLOAD = """
            INSERT INTO pending_files_uploads (key, owner_user_id, uploaded_at)
            VALUES (:key, :ownerUserId, :uploadedAt)
            """;

    //language=postgresql
    private static final String SELECT_COUNT_BY_USER_ID = """
            SELECT COUNT(*)
            FROM pending_files_uploads
            WHERE owner_user_id = :ownerUserId
            """;

    //language=postgresql
    private static final String SELECT_OLD_UPLOADS = """
            SELECT key
            FROM pending_files_uploads
            WHERE uploaded_at < :maxUploadedAt
            ORDER BY uploaded_at ASC
            LIMIT 1000 OFFSET :offset
            """;

    //language=postgresql
    private static final String DELETE_UPLOADS = """
            DELETE FROM pending_files_uploads
            WHERE key IN (:keys)
            """;

    //language=postgresql
    private static final String FILTER_UPLOADS_BY_ACCESS = """
            SELECT key
            FROM pending_files_uploads
            WHERE owner_user_id = :ownerUserId AND key IN (:keys)
            """;

    private final DatabaseClient databaseClient;

    public UploadRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    public Mono<Long> saveUserUpload(String key, long userId, long uploadedAt) {
        return databaseClient.sql(SAVE_USER_UPLOAD)
                .bind("key", key)
                .bind("ownerUserId", userId)
                .bind("uploadedAt", uploadedAt)
                .fetch()
                .rowsUpdated();
    }

    public Mono<Integer> countUserUploads(long userId) {
        return databaseClient.sql(SELECT_COUNT_BY_USER_ID)
                .bind("ownerUserId", userId)
                .map(row -> row.get(0, Integer.class))
                .one();
    }

    public Flux<String> findOldUploads(long maxUploadedAt, int offset) {
        return databaseClient.sql(SELECT_OLD_UPLOADS)
                .bind("maxUploadedAt", maxUploadedAt)
                .bind("offset", offset)
                .map(row -> row.get(0, String.class))
                .all();
    }

    public Mono<Long> deleteUploads(List<String> keys) {
        return databaseClient.sql(DELETE_UPLOADS)
                .bind("keys", keys)
                .fetch()
                .rowsUpdated();
    }

    public Flux<String> filterUploadsByAccess(List<String> keys, long userId) {
        return databaseClient.sql(FILTER_UPLOADS_BY_ACCESS)
                .bind("ownerUserId", userId)
                .bind("keys", keys)
                .map(row -> row.get(0, String.class))
                .all();
    }
}
