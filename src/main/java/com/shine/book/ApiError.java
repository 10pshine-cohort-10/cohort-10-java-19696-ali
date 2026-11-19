package com.shine.book;

import org.springframework.http.HttpStatus;

class ApiError extends RuntimeException {
    final HttpStatus status;
    ApiError(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
    static ApiError unauthorized() {
        return new ApiError(HttpStatus.UNAUTHORIZED, "Please sign in to continue.");
    }
}
