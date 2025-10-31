package com.magnab.employeelifecycle.controller;

import com.magnab.employeelifecycle.dto.request.EmployeeDetails;
import com.magnab.employeelifecycle.dto.request.InitiateWorkflowRequest;
import com.magnab.employeelifecycle.dto.response.TaskAssignmentResult;
import com.magnab.employeelifecycle.dto.response.WorkflowCreationResult;
import com.magnab.employeelifecycle.dto.response.WorkflowInitiationResponse;
import com.magnab.employeelifecycle.entity.User;
import com.magnab.employeelifecycle.enums.UserRole;
import com.magnab.employeelifecycle.enums.WorkflowStatus;
import com.magnab.employeelifecycle.exception.ResourceNotFoundException;
import com.magnab.employeelifecycle.exception.UnauthorizedException;
import com.magnab.employeelifecycle.exception.ValidationException;
import com.magnab.employeelifecycle.service.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowController Unit Tests")
class WorkflowControllerTest {

    @Mock
    private WorkflowService workflowService;

    @InjectMocks
    private WorkflowController workflowController;

    private UUID templateId;
    private UUID workflowInstanceId;
    private UUID userId;
    private InitiateWorkflowRequest validRequest;

    @BeforeEach
    void setUp() {
        templateId = UUID.randomUUID();
        workflowInstanceId = UUID.randomUUID();
        userId = UUID.randomUUID();

        // Create valid request
        validRequest = new InitiateWorkflowRequest();
        validRequest.setTemplateId(templateId);
        validRequest.setEmployeeName("John Doe");
        validRequest.setEmployeeEmail("john.doe@company.com");
        validRequest.setEmployeeRole("Software Engineer");
        validRequest.setCustomFieldValues(Map.of("startDate", "2025-02-01"));

        // Setup security context with HR_ADMIN user
        setupSecurityContext(UserRole.HR_ADMIN);
    }

    private void setupSecurityContext(UserRole role) {
        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setUsername("test.user");
        mockUser.setRole(role);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                mockUser,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );

        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Should return 201 Created with workflow details when valid request")
    void initiateWorkflow_ValidRequest_Returns201Created() {
        // Arrange
        WorkflowCreationResult creationResult = new WorkflowCreationResult();
        creationResult.setWorkflowInstanceId(workflowInstanceId);
        creationResult.setTotalTasks(15);
        creationResult.setImmediateTasksCount(15);

        TaskAssignmentResult assignmentResult = new TaskAssignmentResult();
        assignmentResult.setTaskInstanceId(UUID.randomUUID());
        assignmentResult.setAssignedUserId(UUID.randomUUID());
        assignmentResult.setAssignedUserEmail("user@example.com");
        assignmentResult.setTaskName("Task 1");
        assignmentResult.setDueDate(LocalDateTime.now().plusDays(7));

        when(workflowService.createWorkflowInstance(any(UUID.class), any(EmployeeDetails.class),
                any(Map.class), any(UUID.class)))
                .thenReturn(creationResult);

        when(workflowService.assignTasksForWorkflow(workflowInstanceId))
                .thenReturn(List.of(assignmentResult, assignmentResult, assignmentResult));

        // Act
        ResponseEntity<WorkflowInitiationResponse> response = workflowController.initiateWorkflow(validRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getWorkflowInstanceId()).isEqualTo(workflowInstanceId);
        assertThat(response.getBody().getEmployeeName()).isEqualTo("John Doe");
        assertThat(response.getBody().getStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);
        assertThat(response.getBody().getTotalTasksCreated()).isEqualTo(15);
        assertThat(response.getBody().getTasksAssigned()).isEqualTo(3);
        assertThat(response.getBody().getInitiatedAt()).isNotNull();

        // Verify service methods called
        ArgumentCaptor<EmployeeDetails> employeeCaptor = ArgumentCaptor.forClass(EmployeeDetails.class);
        verify(workflowService).createWorkflowInstance(
                eq(templateId),
                employeeCaptor.capture(),
                eq(validRequest.getCustomFieldValues()),
                eq(userId)
        );
        verify(workflowService).assignTasksForWorkflow(workflowInstanceId);

        // Verify EmployeeDetails populated correctly
        EmployeeDetails capturedDetails = employeeCaptor.getValue();
        assertThat(capturedDetails.getEmployeeName()).isEqualTo("John Doe");
        assertThat(capturedDetails.getEmployeeEmail()).isEqualTo("john.doe@company.com");
        assertThat(capturedDetails.getEmployeeRole()).isEqualTo("Software Engineer");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when template not found")
    void initiateWorkflow_TemplateNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(workflowService.createWorkflowInstance(any(UUID.class), any(EmployeeDetails.class),
                any(Map.class), any(UUID.class)))
                .thenThrow(new ResourceNotFoundException("Workflow template with ID " + templateId + " not found"));

        // Act & Assert
        assertThatThrownBy(() -> workflowController.initiateWorkflow(validRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");

        verify(workflowService).createWorkflowInstance(any(UUID.class), any(EmployeeDetails.class),
                any(Map.class), any(UUID.class));
        verify(workflowService, never()).assignTasksForWorkflow(any(UUID.class));
    }

    @Test
    @DisplayName("Should throw ValidationException when service validation fails")
    void initiateWorkflow_ServiceThrowsValidationException_ThrowsValidationException() {
        // Arrange
        when(workflowService.createWorkflowInstance(any(UUID.class), any(EmployeeDetails.class),
                any(Map.class), any(UUID.class)))
                .thenThrow(new ValidationException("Required custom field 'startDate' not provided"));

        // Act & Assert
        assertThatThrownBy(() -> workflowController.initiateWorkflow(validRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("startDate");

        verify(workflowService).createWorkflowInstance(any(UUID.class), any(EmployeeDetails.class),
                any(Map.class), any(UUID.class));
        verify(workflowService, never()).assignTasksForWorkflow(any(UUID.class));
    }

    @Test
    @DisplayName("Should throw ValidationException when template is inactive")
    void initiateWorkflow_TemplateInactive_ThrowsValidationException() {
        // Arrange
        when(workflowService.createWorkflowInstance(any(UUID.class), any(EmployeeDetails.class),
                any(Map.class), any(UUID.class)))
                .thenThrow(new ValidationException("Workflow template with ID " + templateId + " is not active"));

        // Act & Assert
        assertThatThrownBy(() -> workflowController.initiateWorkflow(validRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not active");

        verify(workflowService).createWorkflowInstance(any(UUID.class), any(EmployeeDetails.class),
                any(Map.class), any(UUID.class));
        verify(workflowService, never()).assignTasksForWorkflow(any(UUID.class));
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when no authenticated user")
    void initiateWorkflow_NoAuthenticatedUser_ThrowsUnauthorizedException() {
        // Arrange - Set up context with null authentication
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        assertThatThrownBy(() -> workflowController.initiateWorkflow(validRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("authenticated");

        verify(workflowService, never()).createWorkflowInstance(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should handle null custom field values")
    void initiateWorkflow_NullCustomFieldValues_Success() {
        // Arrange
        validRequest.setCustomFieldValues(null);

        WorkflowCreationResult creationResult = new WorkflowCreationResult();
        creationResult.setWorkflowInstanceId(workflowInstanceId);
        creationResult.setTotalTasks(5);
        creationResult.setImmediateTasksCount(5);

        when(workflowService.createWorkflowInstance(any(UUID.class), any(EmployeeDetails.class),
                any(), any(UUID.class)))
                .thenReturn(creationResult);

        when(workflowService.assignTasksForWorkflow(workflowInstanceId))
                .thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<WorkflowInitiationResponse> response = workflowController.initiateWorkflow(validRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTasksAssigned()).isEqualTo(0);

        verify(workflowService).createWorkflowInstance(eq(templateId), any(EmployeeDetails.class),
                eq(null), eq(userId));
    }

    @Test
    @DisplayName("Should handle empty custom field values")
    void initiateWorkflow_EmptyCustomFieldValues_Success() {
        // Arrange
        validRequest.setCustomFieldValues(Collections.emptyMap());

        WorkflowCreationResult creationResult = new WorkflowCreationResult();
        creationResult.setWorkflowInstanceId(workflowInstanceId);
        creationResult.setTotalTasks(5);
        creationResult.setImmediateTasksCount(5);

        when(workflowService.createWorkflowInstance(any(UUID.class), any(EmployeeDetails.class),
                any(Map.class), any(UUID.class)))
                .thenReturn(creationResult);

        when(workflowService.assignTasksForWorkflow(workflowInstanceId))
                .thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<WorkflowInitiationResponse> response = workflowController.initiateWorkflow(validRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(workflowService).createWorkflowInstance(any(), any(), any(Map.class), any());
    }

    @Test
    @DisplayName("Should verify both service methods are called in correct order")
    void initiateWorkflow_ServiceMethodsCalledInOrder() {
        // Arrange
        WorkflowCreationResult creationResult = new WorkflowCreationResult();
        creationResult.setWorkflowInstanceId(workflowInstanceId);
        creationResult.setTotalTasks(10);
        creationResult.setImmediateTasksCount(10);

        when(workflowService.createWorkflowInstance(any(UUID.class), any(EmployeeDetails.class),
                any(Map.class), any(UUID.class)))
                .thenReturn(creationResult);

        when(workflowService.assignTasksForWorkflow(workflowInstanceId))
                .thenReturn(Collections.emptyList());

        // Act
        workflowController.initiateWorkflow(validRequest);

        // Assert - Verify order of invocation
        var inOrder = inOrder(workflowService);
        inOrder.verify(workflowService).createWorkflowInstance(any(UUID.class), any(EmployeeDetails.class),
                any(Map.class), any(UUID.class));
        inOrder.verify(workflowService).assignTasksForWorkflow(workflowInstanceId);
    }
}
