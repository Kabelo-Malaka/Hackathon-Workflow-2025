package com.magnab.employeelifecycle.controller;

import com.magnab.employeelifecycle.dto.request.EmployeeDetails;
import com.magnab.employeelifecycle.dto.request.InitiateWorkflowRequest;
import com.magnab.employeelifecycle.dto.response.*;
import com.magnab.employeelifecycle.enums.WorkflowType;
import com.magnab.employeelifecycle.exception.ForbiddenException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

    // ========== GET /api/workflows Tests ==========

    @Test
    @DisplayName("GET /api/workflows - Should return paginated workflows for HR_ADMIN")
    void getWorkflows_HrAdmin_ReturnsAllWorkflows() {
        // Arrange
        WorkflowSummaryResponse summary1 = new WorkflowSummaryResponse();
        summary1.setId(UUID.randomUUID());
        summary1.setEmployeeName("John Doe");
        summary1.setWorkflowType(WorkflowType.ONBOARDING);
        summary1.setStatus(WorkflowStatus.IN_PROGRESS);
        summary1.setInitiatedAt(LocalDateTime.now());
        summary1.setTotalTasks(15);
        summary1.setCompletedTasks(5);

        WorkflowSummaryResponse summary2 = new WorkflowSummaryResponse();
        summary2.setId(UUID.randomUUID());
        summary2.setEmployeeName("Jane Smith");
        summary2.setWorkflowType(WorkflowType.OFFBOARDING);
        summary2.setStatus(WorkflowStatus.COMPLETED);
        summary2.setInitiatedAt(LocalDateTime.now().minusDays(5));
        summary2.setTotalTasks(12);
        summary2.setCompletedTasks(12);

        Page<WorkflowSummaryResponse> expectedPage = new PageImpl<>(List.of(summary1, summary2));

        when(workflowService.getWorkflows(
                any(), any(), any(), any(), any(), anyInt(), anyInt(), any(UUID.class), any(UserRole.class)
        )).thenReturn(expectedPage);

        // Act
        ResponseEntity<Page<WorkflowSummaryResponse>> response = workflowController.getWorkflows(
                null, null, null, "initiatedAt", "desc", 0, 50
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(2);
        assertThat(response.getBody().getContent().get(0).getEmployeeName()).isEqualTo("John Doe");
        assertThat(response.getBody().getContent().get(1).getEmployeeName()).isEqualTo("Jane Smith");

        verify(workflowService).getWorkflows(
                null, null, null, "initiatedAt", "desc", 0, 50, userId, UserRole.HR_ADMIN
        );
    }

    @Test
    @DisplayName("GET /api/workflows - Should apply filters correctly")
    void getWorkflows_WithFilters_AppliesFilters() {
        // Arrange
        Page<WorkflowSummaryResponse> expectedPage = new PageImpl<>(Collections.emptyList());

        when(workflowService.getWorkflows(
                eq("IN_PROGRESS"), eq("ONBOARDING"), eq("John"), any(), any(), anyInt(), anyInt(),
                any(UUID.class), any(UserRole.class)
        )).thenReturn(expectedPage);

        // Act
        ResponseEntity<Page<WorkflowSummaryResponse>> response = workflowController.getWorkflows(
                "IN_PROGRESS", "ONBOARDING", "John", "employeeName", "asc", 1, 20
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(workflowService).getWorkflows(
                "IN_PROGRESS", "ONBOARDING", "John", "employeeName", "asc", 1, 20, userId, UserRole.HR_ADMIN
        );
    }

    @Test
    @DisplayName("GET /api/workflows - Should return workflows for non-admin user")
    void getWorkflows_NonAdminUser_ReturnsUserWorkflows() {
        // Arrange
        setupSecurityContext(UserRole.TECH_SUPPORT);

        WorkflowSummaryResponse summary = new WorkflowSummaryResponse();
        summary.setId(UUID.randomUUID());
        summary.setEmployeeName("John Doe");
        summary.setWorkflowType(WorkflowType.ONBOARDING);
        summary.setStatus(WorkflowStatus.IN_PROGRESS);

        Page<WorkflowSummaryResponse> expectedPage = new PageImpl<>(List.of(summary));

        when(workflowService.getWorkflows(
                any(), any(), any(), any(), any(), anyInt(), anyInt(), any(UUID.class), eq(UserRole.TECH_SUPPORT)
        )).thenReturn(expectedPage);

        // Act
        ResponseEntity<Page<WorkflowSummaryResponse>> response = workflowController.getWorkflows(
                null, null, null, "initiatedAt", "desc", 0, 50
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);

        verify(workflowService).getWorkflows(
                null, null, null, "initiatedAt", "desc", 0, 50, userId, UserRole.TECH_SUPPORT
        );
    }

    @Test
    @DisplayName("GET /api/workflows - Should return empty page when no workflows")
    void getWorkflows_NoWorkflows_ReturnsEmptyPage() {
        // Arrange
        Page<WorkflowSummaryResponse> emptyPage = new PageImpl<>(Collections.emptyList());

        when(workflowService.getWorkflows(
                any(), any(), any(), any(), any(), anyInt(), anyInt(), any(UUID.class), any(UserRole.class)
        )).thenReturn(emptyPage);

        // Act
        ResponseEntity<Page<WorkflowSummaryResponse>> response = workflowController.getWorkflows(
                null, null, null, "initiatedAt", "desc", 0, 50
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isEmpty();
    }

    @Test
    @DisplayName("GET /api/workflows - Should throw UnauthorizedException when not authenticated")
    void getWorkflows_NoAuthentication_ThrowsUnauthorizedException() {
        // Arrange
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        assertThatThrownBy(() -> workflowController.getWorkflows(
                null, null, null, "initiatedAt", "desc", 0, 50
        ))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("authenticated");

        verify(workflowService, never()).getWorkflows(
                any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), any()
        );
    }

    // ========== GET /api/workflows/{id} Tests ==========

    @Test
    @DisplayName("GET /api/workflows/{id} - Should return workflow details for HR_ADMIN")
    void getWorkflowById_HrAdmin_ReturnsWorkflowDetails() {
        // Arrange
        UUID workflowId = UUID.randomUUID();

        WorkflowDetailResponse expectedResponse = new WorkflowDetailResponse();
        expectedResponse.setId(workflowId);
        expectedResponse.setEmployeeName("John Doe");
        expectedResponse.setEmployeeEmail("john.doe@company.com");
        expectedResponse.setEmployeeRole("Software Engineer");
        expectedResponse.setWorkflowType(WorkflowType.ONBOARDING);
        expectedResponse.setStatus(WorkflowStatus.IN_PROGRESS);
        expectedResponse.setInitiatedAt(LocalDateTime.now());
        expectedResponse.setCustomFieldValues(Map.of("startDate", "2025-02-01"));
        expectedResponse.setTasks(new ArrayList<>());
        expectedResponse.setStateHistory(new ArrayList<>());

        when(workflowService.getWorkflowById(eq(workflowId), any(UUID.class), any(UserRole.class)))
                .thenReturn(expectedResponse);

        // Act
        ResponseEntity<WorkflowDetailResponse> response = workflowController.getWorkflowById(workflowId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(workflowId);
        assertThat(response.getBody().getEmployeeName()).isEqualTo("John Doe");
        assertThat(response.getBody().getWorkflowType()).isEqualTo(WorkflowType.ONBOARDING);
        assertThat(response.getBody().getTasks()).isNotNull();
        assertThat(response.getBody().getStateHistory()).isNotNull();

        verify(workflowService).getWorkflowById(workflowId, userId, UserRole.HR_ADMIN);
    }

    @Test
    @DisplayName("GET /api/workflows/{id} - Should return workflow details for authorized non-admin")
    void getWorkflowById_AuthorizedNonAdmin_ReturnsWorkflowDetails() {
        // Arrange
        setupSecurityContext(UserRole.TECH_SUPPORT);
        UUID workflowId = UUID.randomUUID();

        WorkflowDetailResponse expectedResponse = new WorkflowDetailResponse();
        expectedResponse.setId(workflowId);
        expectedResponse.setEmployeeName("Jane Smith");
        expectedResponse.setWorkflowType(WorkflowType.ONBOARDING);
        expectedResponse.setStatus(WorkflowStatus.IN_PROGRESS);
        expectedResponse.setTasks(new ArrayList<>());
        expectedResponse.setStateHistory(new ArrayList<>());

        when(workflowService.getWorkflowById(eq(workflowId), any(UUID.class), eq(UserRole.TECH_SUPPORT)))
                .thenReturn(expectedResponse);

        // Act
        ResponseEntity<WorkflowDetailResponse> response = workflowController.getWorkflowById(workflowId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(workflowId);

        verify(workflowService).getWorkflowById(workflowId, userId, UserRole.TECH_SUPPORT);
    }

    @Test
    @DisplayName("GET /api/workflows/{id} - Should throw ResourceNotFoundException when workflow not found")
    void getWorkflowById_WorkflowNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        UUID workflowId = UUID.randomUUID();

        when(workflowService.getWorkflowById(eq(workflowId), any(UUID.class), any(UserRole.class)))
                .thenThrow(new ResourceNotFoundException("Workflow not found with id: " + workflowId));

        // Act & Assert
        assertThatThrownBy(() -> workflowController.getWorkflowById(workflowId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");

        verify(workflowService).getWorkflowById(workflowId, userId, UserRole.HR_ADMIN);
    }

    @Test
    @DisplayName("GET /api/workflows/{id} - Should throw ForbiddenException for unauthorized non-admin")
    void getWorkflowById_UnauthorizedNonAdmin_ThrowsForbiddenException() {
        // Arrange
        setupSecurityContext(UserRole.TECH_SUPPORT);
        UUID workflowId = UUID.randomUUID();

        when(workflowService.getWorkflowById(eq(workflowId), any(UUID.class), eq(UserRole.TECH_SUPPORT)))
                .thenThrow(new ForbiddenException("Access denied: You are not authorized to view this workflow"));

        // Act & Assert
        assertThatThrownBy(() -> workflowController.getWorkflowById(workflowId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("not authorized");

        verify(workflowService).getWorkflowById(workflowId, userId, UserRole.TECH_SUPPORT);
    }

    @Test
    @DisplayName("GET /api/workflows/{id} - Should throw UnauthorizedException when not authenticated")
    void getWorkflowById_NoAuthentication_ThrowsUnauthorizedException() {
        // Arrange
        UUID workflowId = UUID.randomUUID();

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        assertThatThrownBy(() -> workflowController.getWorkflowById(workflowId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("authenticated");

        verify(workflowService, never()).getWorkflowById(any(), any(), any());
    }

    @Test
    @DisplayName("GET /api/workflows/{id} - Should include tasks and state history in response")
    void getWorkflowById_IncludesTasksAndStateHistory() {
        // Arrange
        UUID workflowId = UUID.randomUUID();

        TaskInstanceSummary task1 = new TaskInstanceSummary();
        task1.setId(UUID.randomUUID());
        task1.setTaskName("Setup laptop");
        task1.setStatus(com.magnab.employeelifecycle.enums.TaskStatus.COMPLETED);

        TaskInstanceSummary task2 = new TaskInstanceSummary();
        task2.setId(UUID.randomUUID());
        task2.setTaskName("Send welcome email");
        task2.setStatus(com.magnab.employeelifecycle.enums.TaskStatus.IN_PROGRESS);

        WorkflowStateHistoryEntry historyEntry = new WorkflowStateHistoryEntry();
        historyEntry.setId(UUID.randomUUID());
        historyEntry.setPreviousStatus(WorkflowStatus.INITIATED);
        historyEntry.setNewStatus(WorkflowStatus.IN_PROGRESS);
        historyEntry.setChangedAt(LocalDateTime.now());

        WorkflowDetailResponse expectedResponse = new WorkflowDetailResponse();
        expectedResponse.setId(workflowId);
        expectedResponse.setEmployeeName("John Doe");
        expectedResponse.setWorkflowType(WorkflowType.ONBOARDING);
        expectedResponse.setStatus(WorkflowStatus.IN_PROGRESS);
        expectedResponse.setTasks(List.of(task1, task2));
        expectedResponse.setStateHistory(List.of(historyEntry));

        when(workflowService.getWorkflowById(eq(workflowId), any(UUID.class), any(UserRole.class)))
                .thenReturn(expectedResponse);

        // Act
        ResponseEntity<WorkflowDetailResponse> response = workflowController.getWorkflowById(workflowId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTasks()).hasSize(2);
        assertThat(response.getBody().getTasks().get(0).getTaskName()).isEqualTo("Setup laptop");
        assertThat(response.getBody().getStateHistory()).hasSize(1);
        assertThat(response.getBody().getStateHistory().get(0).getNewStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);
    }
}
