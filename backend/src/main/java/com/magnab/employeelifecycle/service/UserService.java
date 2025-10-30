package com.magnab.employeelifecycle.service;

import com.magnab.employeelifecycle.dto.request.ChangePasswordRequest;
import com.magnab.employeelifecycle.dto.request.CreateUserRequest;
import com.magnab.employeelifecycle.dto.request.UpdateUserRequest;
import com.magnab.employeelifecycle.dto.response.UserResponse;
import com.magnab.employeelifecycle.entity.User;
import com.magnab.employeelifecycle.exception.ConflictException;
import com.magnab.employeelifecycle.exception.ResourceNotFoundException;
import com.magnab.employeelifecycle.exception.ValidationException;
import com.magnab.employeelifecycle.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating user with username: {}", request.getUsername());

        // Check username uniqueness
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ConflictException("Username already exists");
        }

        // Check email uniqueness
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ConflictException("Email already exists");
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setIsActive(true);

        // Set audit fields
        User currentUser = getCurrentUser();
        user.setCreatedBy(currentUser.getId());
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getId());

        // Log audit event
        String metadata = String.format("{\"username\":\"%s\",\"email\":\"%s\",\"role\":\"%s\"}",
                savedUser.getUsername(), savedUser.getEmail(), savedUser.getRole());
        auditService.logUserAction(savedUser.getId(), "USER_CREATED",
                "User created by " + currentUser.getUsername(), metadata);

        return mapToUserResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        log.info("Fetching all users");
        // TODO: Add pagination support for production (consider Page<UserResponse> with Pageable parameter)
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        log.info("Fetching user by ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        log.info("Updating user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Check email uniqueness if changed
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new ConflictException("Email already exists");
            }
            user.setEmail(request.getEmail());
        }

        // Update fields if provided
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }

        // Set audit fields
        User currentUser = getCurrentUser();
        user.setUpdatedBy(currentUser.getId());
        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully with ID: {}", updatedUser.getId());

        // Log audit event
        String metadata = String.format("{\"email\":\"%s\",\"role\":\"%s\",\"isActive\":%b}",
                updatedUser.getEmail(), updatedUser.getRole(), updatedUser.getIsActive());
        auditService.logUserAction(updatedUser.getId(), "USER_UPDATED",
                "User updated by " + currentUser.getUsername(), metadata);

        return mapToUserResponse(updatedUser);
    }

    @Transactional
    public void deactivateUser(UUID id) {
        log.info("Deactivating user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setIsActive(false);

        // Set audit fields
        User currentUser = getCurrentUser();
        user.setUpdatedBy(currentUser.getId());
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
        log.info("User deactivated successfully with ID: {}", id);

        // Log audit event
        String metadata = String.format("{\"username\":\"%s\"}",user.getUsername());
        auditService.logUserAction(user.getId(), "USER_DEACTIVATED",
                "User deactivated by " + currentUser.getUsername(), metadata);
    }

    @Transactional
    public void changePassword(UUID id, ChangePasswordRequest request) {
        log.info("Changing password for user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ValidationException("Current password is incorrect");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        // Set audit fields
        User currentUser = getCurrentUser();
        user.setUpdatedBy(currentUser.getId());
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
        log.info("Password changed successfully for user with ID: {}", id);

        // Log audit event
        String metadata = String.format("{\"username\":\"%s\"}", user.getUsername());
        auditService.logUserAction(user.getId(), "USER_PASSWORD_CHANGED",
                "Password changed by " + currentUser.getUsername(), metadata);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        return userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Current authenticated user not found: " + currentUsername));
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setIsActive(user.getIsActive());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
}
