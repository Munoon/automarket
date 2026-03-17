package edu.automarket.user;

import edu.automarket.authentication.AuthenticationService;
import edu.automarket.user.dto.AuthRequestDTO;
import edu.automarket.user.dto.AuthResponseDTO;
import edu.automarket.user.dto.RegisterRequestDTO;
import edu.automarket.user.dto.UserDTO;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final AuthenticationService authenticationService;

    public UserController(UserService userService, AuthenticationService authenticationService) {
        this.userService = userService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        return userService.register(request).map(UserDTO::new);
    }

    @PostMapping("/auth")
    public Mono<AuthResponseDTO> authenticate(@Valid @RequestBody AuthRequestDTO request) {
        return userService.getUserByUsername(request.username())
                .filter(user -> user.active() && user.passwordHash().equals(request.passwordHash()))
                .map(user -> {
                    String jwtToken = authenticationService.generateToken(user.id());
                    long tokenExpiresInSeconds = authenticationService.tokenExpirationSeconds();
                    return new AuthResponseDTO(jwtToken, tokenExpiresInSeconds, user);
                })
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")));
    }

    @GetMapping("/profile")
    public Mono<UserDTO> getProfile(@AuthenticationPrincipal Long userId) {
        return userService.getUserByIdOrThrow(userId).map(UserDTO::new);
    }
}
