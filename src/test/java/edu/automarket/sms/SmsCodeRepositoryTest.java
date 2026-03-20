package edu.automarket.sms;

import edu.automarket.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

class SmsCodeRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private SmsCodeRepository smsCodeRepository;

    @Test
    void saveCodeCompletesSuccessfully() {
        StepVerifier.create(smsCodeRepository.saveCode("+380123456789", "123456"))
                .verifyComplete();
    }

    @Test
    void validateSmsCodeAndDeleteReturnsTrueAndDeletesCode() {
        smsCodeRepository.saveCode("+380123456789", "123456").block();

        StepVerifier.create(smsCodeRepository.validateSmsCodeAndDelete("+380123456789", "123456", 0L))
                .expectNext(true)
                .verifyComplete();

        // Code was deleted - second validation returns false
        StepVerifier.create(smsCodeRepository.validateSmsCodeAndDelete("+380123456789", "123456", 0L))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void validateSmsCodeAndDeleteReturnsFalseForWrongCode() {
        smsCodeRepository.saveCode("+380123456789", "123456").block();

        StepVerifier.create(smsCodeRepository.validateSmsCodeAndDelete("+380123456789", "000000", 0L))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void validateSmsCodeAndDeleteReturnsFalseForExpiredCode() {
        smsCodeRepository.saveCode("+380123456789", "123456").block();

        long futureMinCreatedAt = System.currentTimeMillis() + 60_000L;
        StepVerifier.create(smsCodeRepository.validateSmsCodeAndDelete("+380123456789", "123456", futureMinCreatedAt))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void validateSmsCodeAndDeleteReturnsFalseForWrongPhoneNumber() {
        smsCodeRepository.saveCode("+380123456789", "123456").block();

        StepVerifier.create(smsCodeRepository.validateSmsCodeAndDelete("+380987654321", "123456", 0L))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void deleteExpiredCodesDeletesExpiredCodes() {
        smsCodeRepository.saveCode("+380123456789", "123456").block();

        long futureMinCreatedAt = System.currentTimeMillis() + 60_000L;
        StepVerifier.create(smsCodeRepository.deleteExpiredCodes(futureMinCreatedAt))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(smsCodeRepository.validateSmsCodeAndDelete("+380123456789", "123456", 0L))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void deleteExpiredCodesDoesNotDeleteFreshCodes() {
        smsCodeRepository.saveCode("+380123456789", "123456").block();

        StepVerifier.create(smsCodeRepository.deleteExpiredCodes(0L))
                .expectNext(0L)
                .verifyComplete();

        StepVerifier.create(smsCodeRepository.validateSmsCodeAndDelete("+380123456789", "123456", 0L))
                .expectNext(true)
                .verifyComplete();
    }
}
