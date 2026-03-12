package edu.kai.automarket.user;

import edu.kai.automarket.security.JwtService;
import edu.kai.automarket.user.dto.AuthRequestDTO;
import edu.kai.automarket.user.dto.AuthResponseDTO;
import edu.kai.automarket.user.dto.RegisterRequestDTO;
import edu.kai.automarket.user.dto.UserDTO;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final JwtService jwtService;

    public UserController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserDTO> register(@RequestBody RegisterRequestDTO request) {
        return userService.register(request).map(UserDTO::new);
    }

    @PostMapping("/auth")
    public Mono<AuthResponseDTO> authenticate(@RequestBody AuthRequestDTO request) {
        return userService.getUserByEmail(request.email())
                .filter(user -> user.active() && user.passwordHash().equals(request.passwordHash()))
                .map(user -> {
                    String jwtToken = jwtService.generateToken(user.id());
                    long tokenExpiresInSeconds = jwtService.tokenExpirationSeconds();
                    return new AuthResponseDTO(jwtToken, tokenExpiresInSeconds, user);
                })
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")));
    }

    @GetMapping("/profile")
    public Mono<UserDTO> getProfile(@AuthenticationPrincipal Long userId) {
        return userService.getUserByIdOrThrow(userId).map(UserDTO::new);
    }
}
