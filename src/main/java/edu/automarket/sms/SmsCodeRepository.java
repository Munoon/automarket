package edu.automarket.sms;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class SmsCodeRepository {
    //language=postgresql
    private static final String INSERT_VERIFICATION_CODE = """
            INSERT INTO sms_verification_codes (phone_number, code, created_at)
            VALUES (:phoneNumber, :code, :createdAt)
            """;

    //language=postgresql
    private static final String DELETE_SMS_CODE_QUERY = """
            DELETE FROM sms_verification_codes
            WHERE phone_number = :phoneNumber AND code = :code AND created_at >= :minCreatedAt
            """;

    //language=postgresql
    private static final String DELETE_EXPIRED_CODES_QUERY = """
            DELETE FROM sms_verification_codes
            WHERE created_at < :minCreatedAt
            """;

    private final DatabaseClient client;

    public SmsCodeRepository(DatabaseClient client) {
        this.client = client;
    }

    public Mono<Void> saveCode(String phoneNumber, String authCode) {
        return client.sql(INSERT_VERIFICATION_CODE)
                     .bind("phoneNumber", phoneNumber)
                     .bind("code", authCode)
                     .bind("createdAt", System.currentTimeMillis())
                     .fetch().rowsUpdated().then();
    }

    public Mono<Long> deleteExpiredCodes(long minCreatedAt) {
        return client.sql(DELETE_EXPIRED_CODES_QUERY)
                     .bind("minCreatedAt", minCreatedAt)
                     .fetch().rowsUpdated();
    }

    public Mono<Boolean> validateSmsCodeAndDelete(String phoneNumber, String smsCode, long minCreatedAt) {
        return client.sql(DELETE_SMS_CODE_QUERY)
                     .bind("phoneNumber", phoneNumber)
                     .bind("code", smsCode)
                     .bind("minCreatedAt", minCreatedAt)
                     .fetch().rowsUpdated().map(count -> count > 0);
    }
}
