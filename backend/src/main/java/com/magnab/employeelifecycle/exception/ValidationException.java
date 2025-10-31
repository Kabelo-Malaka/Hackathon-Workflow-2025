package com.magnab.employeelifecycle.exception;

/**
 * Exception thrown when template or business logic validation fails.
 * Handled by GlobalExceptionHandler and returned as 400 Bad Request.
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
