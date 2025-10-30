package com.magnab.employeelifecycle.service;

import com.magnab.employeelifecycle.dto.request.CreateUserRequest;
import com.magnab.employeelifecycle.dto.response.UserResponse;
import com.magnab.employeelifecycle.entity.User;
import com.magnab.employeelifecycle.enums.UserRole;
import com.magnab.employeelifecycle.exception.ConflictException;
import com.magnab.employeelifecycle.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditService auditService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserService userService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        // Setup current authenticated user for audit fields
        currentUser = new User();
        currentUser.setId(UUID.randomUUID());
        currentUser.setUsername("admin");
        currentUser.setRole(UserRole.ADMINISTRATOR);

        // Mock Security Context (lenient to avoid unnecessary stubbing warnings)
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("admin");
        SecurityContextHolder.setContext(securityContext);
        lenient().when(userRepository.findByUsername("admin")).thenReturn(Optional.of(currentUser));
    }

    @Test
    void createUser_WithValidData_ReturnsUserResponse() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setEmail("test@magnab.com");
        request.setPassword("password123");
        request.setRole(UserRole.HR_ADMIN);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@magnab.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");

        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setUsername("testuser");
        savedUser.setEmail("test@magnab.com");
        savedUser.setPasswordHash("hashedPassword");
        savedUser.setRole(UserRole.HR_ADMIN);
        savedUser.setIsActive(true);
        savedUser.setCreatedAt(LocalDateTime.now());
        savedUser.setCreatedBy(currentUser.getId());

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        UserResponse result = userService.createUser(request);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@magnab.com", result.getEmail());
        assertEquals(UserRole.HR_ADMIN, result.getRole());
        assertTrue(result.getIsActive());

        verify(userRepository).findByUsername("testuser");
        verify(userRepository).findByEmail("test@magnab.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
        verify(auditService).logUserAction(any(UUID.class), eq("USER_CREATED"), anyString(), anyString());
    }

    @Test
    void createUser_WithDuplicateUsername_ThrowsConflictException() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("existinguser");
        request.setEmail("new@magnab.com");
        request.setPassword("password123");
        request.setRole(UserRole.LINE_MANAGER);

        User existingUser = new User();
        existingUser.setUsername("existinguser");
        when(userRepository.findByUsername("existinguser")).thenReturn(Optional.of(existingUser));

        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class, () -> {
            userService.createUser(request);
        });

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository).findByUsername("existinguser");
        verify(userRepository, never()).save(any(User.class));
        verify(auditService, never()).logUserAction(any(), any(), any(), any());
    }

    @Test
    void createUser_WithDuplicateEmail_ThrowsConflictException() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser");
        request.setEmail("existing@magnab.com");
        request.setPassword("password123");
        request.setRole(UserRole.TECH_SUPPORT);

        User existingUser = new User();
        existingUser.setEmail("existing@magnab.com");
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("existing@magnab.com")).thenReturn(Optional.of(existingUser));

        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class, () -> {
            userService.createUser(request);
        });

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository).findByUsername("newuser");
        verify(userRepository).findByEmail("existing@magnab.com");
        verify(userRepository, never()).save(any(User.class));
        verify(auditService, never()).logUserAction(any(), any(), any(), any());
    }

    @Test
    void getAllUsers_ReturnsListOfUserResponses() {
        // Arrange
        User user1 = new User();
        user1.setId(UUID.randomUUID());
        user1.setUsername("user1");
        user1.setEmail("user1@magnab.com");
        user1.setRole(UserRole.HR_ADMIN);
        user1.setIsActive(true);

        User user2 = new User();
        user2.setId(UUID.randomUUID());
        user2.setUsername("user2");
        user2.setEmail("user2@magnab.com");
        user2.setRole(UserRole.LINE_MANAGER);
        user2.setIsActive(true);

        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

        // Act
        List<UserResponse> result = userService.getAllUsers();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("user1", result.get(0).getUsername());
        assertEquals("user2", result.get(1).getUsername());
        verify(userRepository).findAll();
    }
}
