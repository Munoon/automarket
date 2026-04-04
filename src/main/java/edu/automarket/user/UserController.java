package edu.automarket.user;

import edu.automarket.authentication.AuthenticationService;
import edu.automarket.common.ApiException;
import edu.automarket.listing.CarListingService;
import edu.automarket.sms.SmsCodeService;
import edu.automarket.user.dto.AuthRequestDTO;
import edu.automarket.user.dto.AuthResponseDTO;
import edu.automarket.user.dto.LimitsDTO;
import edu.automarket.user.dto.ProfileResponseDTO;
import edu.automarket.user.dto.SendVerificationCodeRequestDTO;
import edu.automarket.user.dto.SendVerificationCodeResponseDTO;
import edu.automarket.user.dto.UpdateDisplayNameRequestDTO;
import edu.automarket.user.dto.UserDTO;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private static final Logger log = LogManager.getLogger(UserController.class);
    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final SmsCodeService smsCodeService;
    private final LimitsDTO limits;

    public UserController(UserService userService,
                          AuthenticationService authenticationService,
                          SmsCodeService smsCodeService,
                          CarListingService carListingService) {
        this.userService = userService;
        this.authenticationService = authenticationService;
        this.smsCodeService = smsCodeService;
        this.limits = new LimitsDTO(
                carListingService.getListingRepublishCooldownMS(),
                carListingService.getListingsCountPerAuthorLimit()
        );
    }

    @PostMapping("/send-verification-code")
    public Mono<SendVerificationCodeResponseDTO> sendVerificationCode(
            @Valid @RequestBody SendVerificationCodeRequestDTO request) {
        return smsCodeService.sendSms(request.phoneNumber())
                .thenReturn(new SendVerificationCodeResponseDTO(smsCodeService.getAuthCodeTTLSeconds()))
                .onErrorMap(e -> {
                    log.error("Failed to send SMS code", e);
                    return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "/problems/sms-send-failed", "Failed to send SMS code");
                });
    }

    @PostMapping("/auth")
    public Mono<AuthResponseDTO> authenticate(@Valid @RequestBody AuthRequestDTO request) {
        return smsCodeService.validateSmsCode(request.phoneNumber(), request.code())
                .then(Mono.defer(() -> userService.getUserByPhoneNumberOrCreate(request.phoneNumber())))
                .map(user -> {
                    if (!user.active()) {
                        throw new ApiException(HttpStatus.UNAUTHORIZED, "/problems/user-not-active", "User is not active");
                    }

                    String jwtToken = authenticationService.generateToken(user.id());
                    long tokenExpiresInSeconds = authenticationService.tokenExpirationSeconds();
                    return new AuthResponseDTO(jwtToken, tokenExpiresInSeconds, user, limits);
                });
    }

    @GetMapping("/profile")
    public Mono<ProfileResponseDTO> getProfile(@AuthenticationPrincipal Long userId) {
        return userService.getUserByIdOrThrow(userId)
                .map(user -> new ProfileResponseDTO(new UserDTO(user), limits));
    }

    @PatchMapping("/display-name")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> updateDisplayName(@AuthenticationPrincipal Long userId,
                                        @Valid @RequestBody UpdateDisplayNameRequestDTO request) {
        return userService.updateDisplayName(userId, request.displayName());
    }
}
