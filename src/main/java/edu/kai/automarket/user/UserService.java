package edu.kai.automarket.user;

import edu.kai.automarket.user.dto.RegisterRequestDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Mono<User> register(RegisterRequestDTO request) {
        return userRepository.existsByUsername(request.username())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Username already in use"));
                    }

                    User user = new User(
                            null,
                            request.username(),
                            request.phoneNumber(),
                            request.passwordHash(),
                            request.displayName(),
                            System.currentTimeMillis(),
                            true
                    );
                    return userRepository.save(user);
                });
    }

    public Mono<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Mono<User> getUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")));
    }
}
