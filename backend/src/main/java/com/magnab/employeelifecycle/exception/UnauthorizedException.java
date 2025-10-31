package com.magnab.employeelifecycle.exception;

/**
 * Exception thrown when user authentication is required but not found.
 * Handled by GlobalExceptionHandler and returned as 401 Unauthorized.
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
