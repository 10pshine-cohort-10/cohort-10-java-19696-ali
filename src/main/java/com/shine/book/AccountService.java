package com.shine.book;

import java.util.Locale;
import java.util.regex.Pattern;
import org.slf4j.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.http.HttpStatus.*;

@Service
class AccountService {
    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    private static final Pattern EMAIL = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");
    private static final Pattern PHONE = Pattern.compile("\\+?[0-9]{7,15}");
    private final AccountRepository accounts;
    private final PasswordHash passwords;
    private final String dummyHash;

    AccountService(AccountRepository accounts, PasswordHash passwords) {
        this.accounts = accounts;
        this.passwords = passwords;
        this.dummyHash = passwords.encode(java.util.UUID.randomUUID().toString());
    }

    static String normalize(String raw) {
        String id = raw.strip();
        if (id.contains("@")) return id.toLowerCase(Locale.ROOT);
        return id.replaceAll("[() .-]", "");
    }

    @Transactional
    Account register(String name, String identifier, String password) {
        String id = normalize(identifier);
        if (!EMAIL.matcher(id).matches() && !PHONE.matcher(id).matches())
            throw new ApiError(BAD_REQUEST, "Enter a valid email address or phone number.");
        try {
            Account saved = accounts.saveAndFlush(new Account(name.strip(), id, passwords.encode(password)));
            log.info("Account registered, accountId={}", saved.id);
            return saved;
        } catch (DataIntegrityViolationException ex) {
            throw new ApiError(CONFLICT, "This sign-in identifier is already registered.");
        }
    }

    Account login(String identifier, String password) {
        Account account = accounts.findByLoginId(normalize(identifier)).orElse(null);
        boolean valid = passwords.matches(password, account == null ? dummyHash : account.passwordHash);
        if (!valid || account == null) throw new ApiError(UNAUTHORIZED, "Incorrect sign-in details.");
        log.info("Login succeeded, accountId={}", account.id);
        return account;
    }

    @Transactional
    Account changePassword(long id, long version, String current, String replacement) {
        Account account = accounts.lockById(id).orElseThrow(ApiError::unauthorized);
        if (account.credentialVersion != version) throw ApiError.unauthorized();
        if (!passwords.matches(current, account.passwordHash))
            throw new ApiError(BAD_REQUEST, "Current password is incorrect.");
        if (current.equals(replacement)) throw new ApiError(BAD_REQUEST, "Choose a different new password.");
        account.passwordHash = passwords.encode(replacement);
        account.credentialVersion++;
        log.info("Password changed, accountId={}", id);
        return account;
    }
}
