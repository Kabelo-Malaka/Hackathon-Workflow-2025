package com.magnab.employeelifecycle.exception;

/**
 * Exception thrown when user is not authorized to access a resource.
 * Handled by GlobalExceptionHandler and returned as 403 Forbidden.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
