# Security

**Authentication:** Session-based (15-minute timeout) with BCrypt password hashing

**Authorization:** Role-based access control (RBAC) enforced at controller level
```java
@PreAuthorize("hasRole('HR_ADMIN')")
@PostMapping("/api/workflows")
public ResponseEntity<WorkflowDetailResponse> initiateWorkflow(...) {
    // Only HR_ADMIN can initiate workflows
}
```

**Input Validation:**
- Jakarta Bean Validation (@Valid on request DTOs)
- Whitelist approach (define allowed values)
- Validation at API boundary before service layer

**CSRF Protection:** Enabled for all non-GET requests (Spring Security default)

**CORS Policy:**
```java
@Configuration
public class SecurityConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost", "http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowCredentials(true);
        return source;
    }
}
```

**Security Headers:**
- X-Frame-Options: DENY
- X-Content-Type-Options: nosniff
- X-XSS-Protection: 1; mode=block

**Secrets Management:**
- **Development:** Environment variables via .env file
- **Production:** Environment variables injected by deployment platform
- **Never:** Hardcoded secrets in code or configuration files

**Data Protection:**
- Encryption in transit: TLS for SMTP (port 587)
- Encryption at rest: PostgreSQL (optional, not configured for MVP)
- Password storage: BCrypt (Spring Security default)

**SQL Injection Prevention:** JPA parameterized queries (automatic)

**Dependency Security:** `npm audit` for frontend, Maven dependency check (optional for MVP)
