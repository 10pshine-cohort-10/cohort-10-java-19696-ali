package com.shine.book;

import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;

@Component
class SessionAccess {
    private static final String USER = "accountId";
    private static final String VERSION = "credentialVersion";
    private final AccountRepository accounts;
    SessionAccess(AccountRepository accounts) { this.accounts = accounts; }

    void start(HttpServletRequest request, Account account) {
        HttpSession previous = request.getSession(false);
        if (previous != null) previous.invalidate();
        HttpSession session = request.getSession(true);
        session.setAttribute(USER, account.id);
        session.setAttribute(VERSION, account.credentialVersion);
    }

    Account require(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(USER) instanceof Long id)) throw ApiError.unauthorized();
        Account account = accounts.findById(id).orElseThrow(ApiError::unauthorized);
        if (!Long.valueOf(account.credentialVersion).equals(session.getAttribute(VERSION))) {
            session.invalidate();
            throw ApiError.unauthorized();
        }
        return account;
    }
}
