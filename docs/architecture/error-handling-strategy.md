# Error Handling Strategy

**Approach:** Centralized exception handling with consistent error responses

**Error Model:**
```java
@Data
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
}
```

**Global Exception Handler:**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex, WebRequest request) {
        return ResponseEntity.status(400).body(new ErrorResponse(...));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        return ResponseEntity.status(404).body(new ErrorResponse(...));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex, WebRequest request) {
        return ResponseEntity.status(403).body(new ErrorResponse(...));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, WebRequest request) {
        return ResponseEntity.status(409).body(new ErrorResponse(...));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, WebRequest request) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(500).body(new ErrorResponse(...));
    }
}
```

**Logging Standards:**
- **Library:** SLF4J + Logback
- **Format:** JSON for structured logging
- **Levels:** ERROR (unrecoverable), WARN (degraded), INFO (key events), DEBUG (diagnostic)
- **Correlation ID:** UUID per request (MDC context)
- **Log files:** Captured by Docker (stdout/stderr)

**Email Retry Logic:**
```java
@Async
@Retryable(
    value = {MailException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public void sendEmail(EmailDetails details) {
    // Send email via JavaMail
    // Log attempt to audit_events
}
```

**Circuit Breaker:** Not needed for MVP (only Gmail SMTP external dependency)
