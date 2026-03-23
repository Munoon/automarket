package edu.automarket.user;

import edu.automarket.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Mono<User> getUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new ApiException(HttpStatus.NOT_FOUND, "/problems/user-not-found", "User not found")));
    }

    public Mono<User> getUserByPhoneNumberOrCreate(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .switchIfEmpty(Mono.defer(() -> userRepository.register(phoneNumber)));
    }

    public Mono<Void> updateDisplayName(Long userId, String displayName) {
        return userRepository.updateDisplayName(userId, displayName);
    }
}
