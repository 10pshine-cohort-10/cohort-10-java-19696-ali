package com.shine.book;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.slf4j.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/account")
class AccountController {
    record Registration(@NotBlank @Size(max=100) String name, @NotBlank @Size(max=254) String identifier,
                        @NotNull @Size(min=10,max=128) String password) {}
    record Login(@NotBlank @Size(max=254) String identifier, @NotBlank @Size(max=128) String password) {}
    record PasswordChange(@NotBlank @Size(max=128) String currentPassword, @NotNull @Size(min=10,max=128) String newPassword) {}
    record Profile(long id, String name, String identifier) {
        static Profile of(Account a) { return new Profile(a.id, a.displayName, a.loginId); }
    }
    private final AccountService service;
    private final SessionAccess sessions;
    private static final Logger log = LoggerFactory.getLogger(AccountController.class);
    AccountController(AccountService service, SessionAccess sessions) {
        this.service = service;
        this.sessions = sessions;
    }

    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    Profile register(@Valid @RequestBody Registration form, HttpServletRequest request) {
        Account account = service.register(form.name(), form.identifier(), form.password());
        sessions.start(request, account);
        return Profile.of(account);
    }

    @PostMapping("/login")
    Profile login(@Valid @RequestBody Login form, HttpServletRequest request) {
        Account account = service.login(form.identifier(), form.password());
        sessions.start(request, account);
        return Profile.of(account);
    }

    @GetMapping
    Profile profile(HttpServletRequest request) { return Profile.of(sessions.require(request)); }

    @PostMapping("/password") @ResponseStatus(HttpStatus.NO_CONTENT)
    void password(@Valid @RequestBody PasswordChange form, HttpServletRequest request) {
        Account a = sessions.require(request);
        sessions.start(request, service.changePassword(a.id, a.credentialVersion, form.currentPassword(), form.newPassword()));
    }

    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletRequest request) {
        Account a = sessions.require(request);
        request.getSession(false).invalidate();
        log.info("Logout, accountId={}", a.id);
    }
}
