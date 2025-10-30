package com.magnab.employeelifecycle.controller;

import com.magnab.employeelifecycle.dto.request.LoginRequest;
import com.magnab.employeelifecycle.dto.response.AuthResponse;
import com.magnab.employeelifecycle.enums.UserRole;
import com.magnab.employeelifecycle.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost", "http://localhost:3000"}, allowCredentials = "true")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;

    public AuthController(AuthenticationManager authenticationManager, AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.auditService = auditService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);

            request.getSession().setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    securityContext
            );

            String username = authentication.getName();
            UserRole role = extractRole(authentication);

            auditService.logAuthenticationAttempt(username, true, ipAddress, null);

            return ResponseEntity.ok(new AuthResponse(username, role, "Login successful"));

        } catch (BadCredentialsException e) {
            auditService.logAuthenticationAttempt(loginRequest.getUsername(), false, ipAddress, null);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            errorResponse.put("status", 401);
            errorResponse.put("error", "Unauthorized");
            errorResponse.put("message", "Invalid credentials");
            errorResponse.put("path", "/api/auth/login");

            return ResponseEntity.status(401).body(errorResponse);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();

            request.getSession().invalidate();
            SecurityContextHolder.clearContext();

            auditService.logLogout(username, null);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Logout successful");
            return ResponseEntity.ok(response);
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Logout successful");
        return ResponseEntity.ok(response);
    }

    private UserRole extractRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.replace("ROLE_", ""))
                .map(UserRole::valueOf)
                .orElseThrow(() -> new IllegalStateException("User has no role assigned"));
    }
}
