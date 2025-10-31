package com.magnab.employeelifecycle.service;

import com.magnab.employeelifecycle.dto.response.*;
import com.magnab.employeelifecycle.entity.*;
import com.magnab.employeelifecycle.enums.*;
import com.magnab.employeelifecycle.exception.ForbiddenException;
import com.magnab.employeelifecycle.exception.ResourceNotFoundException;
import com.magnab.employeelifecycle.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowService Retrieval Methods Unit Tests")
class WorkflowServiceRetrievalTest {

    @Mock
    private WorkflowInstanceRepository workflowInstanceRepository;

    @Mock
    private TaskInstanceRepository taskInstanceRepository;

    @Mock
    private WorkflowStateHistoryRepository workflowStateHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WorkflowService workflowService;

    private UUID userId;
    private UUID workflowId;
    private WorkflowInstance workflowInstance;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        workflowId = UUID.randomUUID();

        // Setup test user
        user = new User();
        user.setId(userId);
        user.setUsername("test.user");
        user.setEmail("test.user@company.com");
        user.setRole(UserRole.HR_ADMIN);

        // Setup test workflow instance
        workflowInstance = new WorkflowInstance();
        workflowInstance.setId(workflowId);
        workflowInstance.setEmployeeName("John Doe");
        workflowInstance.setEmployeeEmail("john.doe@company.com");
        workflowInstance.setEmployeeRole("Software Engineer");
        workflowInstance.setWorkflowType(WorkflowType.ONBOARDING);
        workflowInstance.setStatus(WorkflowStatus.IN_PROGRESS);
        workflowInstance.setInitiatedAt(LocalDateTime.now());
        workflowInstance.setInitiatedBy(userId);
        workflowInstance.setCustomFieldValues(Map.of("startDate", "2025-02-01"));
    }

    // ========== getWorkflows() Tests ==========

    @Test
    @DisplayName("getWorkflows - HR_ADMIN should see all workflows")
    void getWorkflows_HrAdmin_ReturnsAllWorkflows() {
        // Arrange
        WorkflowInstance workflow2 = new WorkflowInstance();
        workflow2.setId(UUID.randomUUID());
        workflow2.setEmployeeName("Jane Smith");
        workflow2.setWorkflowType(WorkflowType.OFFBOARDING);
        workflow2.setStatus(WorkflowStatus.COMPLETED);
        workflow2.setInitiatedAt(LocalDateTime.now().minusDays(5));

        Page<WorkflowInstance> mockPage = new PageImpl<>(List.of(workflowInstance, workflow2));

        when(workflowInstanceRepository.findAllByFilters(
                any(), any(), any(), any(Pageable.class)
        )).thenReturn(mockPage);

        when(taskInstanceRepository.findByWorkflowInstanceId(workflowInstance.getId()))
                .thenReturn(createMockTasksWithStatus(15, 5));
        when(taskInstanceRepository.findByWorkflowInstanceId(workflow2.getId()))
                .thenReturn(createMockTasksWithStatus(12, 12));

        // Act
        Page<WorkflowSummaryResponse> result = workflowService.getWorkflows(
                null, null, null, "initiatedAt", "desc", 0, 50, userId, UserRole.HR_ADMIN
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getEmployeeName()).isEqualTo("John Doe");
        assertThat(result.getContent().get(1).getEmployeeName()).isEqualTo("Jane Smith");

        verify(workflowInstanceRepository).findAllByFilters(any(), any(), any(), any(Pageable.class));
        verify(workflowInstanceRepository, never()).findByUserHasAssignedTasks(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("getWorkflows - Non-admin should see only their workflows")
    void getWorkflows_NonAdmin_ReturnsUserWorkflows() {
        // Arrange
        Page<WorkflowInstance> mockPage = new PageImpl<>(List.of(workflowInstance));

        when(workflowInstanceRepository.findByUserHasAssignedTasks(
                eq(userId), any(), any(), any(), any(Pageable.class)
        )).thenReturn(mockPage);

        when(taskInstanceRepository.findByWorkflowInstanceId(workflowInstance.getId()))
                .thenReturn(createMockTasksWithStatus(15, 5));

        // Act
        Page<WorkflowSummaryResponse> result = workflowService.getWorkflows(
                null, null, null, "initiatedAt", "desc", 0, 50, userId, UserRole.TECH_SUPPORT
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmployeeName()).isEqualTo("John Doe");

        verify(workflowInstanceRepository).findByUserHasAssignedTasks(eq(userId), any(), any(), any(), any(Pageable.class));
        verify(workflowInstanceRepository, never()).findAllByFilters(any(), any(), any(), any());
    }

    @Test
    @DisplayName("getWorkflows - Should apply status filter")
    void getWorkflows_WithStatusFilter_AppliesFilter() {
        // Arrange
        Page<WorkflowInstance> mockPage = new PageImpl<>(Collections.emptyList());

        when(workflowInstanceRepository.findAllByFilters(
                eq(WorkflowStatus.IN_PROGRESS), any(), any(), any(Pageable.class)
        )).thenReturn(mockPage);

        // Act
        Page<WorkflowSummaryResponse> result = workflowService.getWorkflows(
                "IN_PROGRESS", null, null, "initiatedAt", "desc", 0, 50, userId, UserRole.HR_ADMIN
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();

        verify(workflowInstanceRepository).findAllByFilters(
                eq(WorkflowStatus.IN_PROGRESS), eq(null), eq(null), any(Pageable.class)
        );
    }

    @Test
    @DisplayName("getWorkflows - Should calculate task counts correctly")
    void getWorkflows_CalculatesTaskCountsCorrectly() {
        // Arrange
        Page<WorkflowInstance> mockPage = new PageImpl<>(List.of(workflowInstance));

        when(workflowInstanceRepository.findAllByFilters(any(), any(), any(), any(Pageable.class)))
                .thenReturn(mockPage);

        when(taskInstanceRepository.findByWorkflowInstanceId(workflowId))
                .thenReturn(createMockTasksWithStatus(15, 8));

        // Act
        Page<WorkflowSummaryResponse> result = workflowService.getWorkflows(
                null, null, null, "initiatedAt", "desc", 0, 50, userId, UserRole.HR_ADMIN
        );

        // Assert
        assertThat(result.getContent().get(0).getTotalTasks()).isEqualTo(15);
        assertThat(result.getContent().get(0).getCompletedTasks()).isEqualTo(8);
    }

    @Test
    @DisplayName("getWorkflows - Should return empty page when no workflows found")
    void getWorkflows_NoWorkflows_ReturnsEmptyPage() {
        // Arrange
        Page<WorkflowInstance> emptyPage = new PageImpl<>(Collections.emptyList());

        when(workflowInstanceRepository.findAllByFilters(any(), any(), any(), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        Page<WorkflowSummaryResponse> result = workflowService.getWorkflows(
                null, null, null, "initiatedAt", "desc", 0, 50, userId, UserRole.HR_ADMIN
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("getWorkflows - Should sort by specified field and direction")
    void getWorkflows_AppliesSortingCorrectly() {
        // Arrange
        Page<WorkflowInstance> mockPage = new PageImpl<>(Collections.emptyList());

        when(workflowInstanceRepository.findAllByFilters(any(), any(), any(), any(Pageable.class)))
                .thenReturn(mockPage);

        // Act
        workflowService.getWorkflows(
                null, null, null, "employeeName", "asc", 0, 50, userId, UserRole.HR_ADMIN
        );

        // Assert - Verify Pageable was created with correct sort
        verify(workflowInstanceRepository).findAllByFilters(
                any(), any(), any(), argThat(pageable ->
                        pageable.getSort().getOrderFor("employeeName") != null &&
                        pageable.getSort().getOrderFor("employeeName").isAscending()
                )
        );
    }

    // ========== getWorkflowById() Tests ==========

    @Test
    @DisplayName("getWorkflowById - HR_ADMIN can view any workflow")
    void getWorkflowById_HrAdmin_ReturnsWorkflowDetails() {
        // Arrange
        when(workflowInstanceRepository.findById(workflowId))
                .thenReturn(Optional.of(workflowInstance));

        List<TaskInstance> mockTasks = createMockTaskInstances(3);
        when(taskInstanceRepository.findByWorkflowInstanceIdOrderBySequenceOrder(workflowId))
                .thenReturn(mockTasks);

        List<WorkflowStateHistory> mockHistory = createMockStateHistory(2);
        when(workflowStateHistoryRepository.findByWorkflowInstanceIdOrderByChangedAtDesc(workflowId))
                .thenReturn(mockHistory);

        when(userRepository.findById(any(UUID.class)))
                .thenReturn(Optional.of(user));

        // Act
        WorkflowDetailResponse result = workflowService.getWorkflowById(
                workflowId, userId, UserRole.HR_ADMIN
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(workflowId);
        assertThat(result.getEmployeeName()).isEqualTo("John Doe");
        assertThat(result.getEmployeeEmail()).isEqualTo("john.doe@company.com");
        assertThat(result.getTasks()).hasSize(3);
        assertThat(result.getStateHistory()).hasSize(2);
        assertThat(result.getCustomFieldValues()).containsEntry("startDate", "2025-02-01");

        verify(taskInstanceRepository, never()).existsByWorkflowInstanceIdAndAssignedUserId(any(), any());
    }

    @Test
    @DisplayName("getWorkflowById - Non-admin with access can view workflow")
    void getWorkflowById_AuthorizedNonAdmin_ReturnsWorkflowDetails() {
        // Arrange
        when(workflowInstanceRepository.findById(workflowId))
                .thenReturn(Optional.of(workflowInstance));

        when(taskInstanceRepository.existsByWorkflowInstanceIdAndAssignedUserId(workflowId, userId))
                .thenReturn(true);

        List<TaskInstance> mockTasks = createMockTaskInstances(2);
        when(taskInstanceRepository.findByWorkflowInstanceIdOrderBySequenceOrder(workflowId))
                .thenReturn(mockTasks);

        when(workflowStateHistoryRepository.findByWorkflowInstanceIdOrderByChangedAtDesc(workflowId))
                .thenReturn(Collections.emptyList());

        when(userRepository.findById(any(UUID.class)))
                .thenReturn(Optional.of(user));

        // Act
        WorkflowDetailResponse result = workflowService.getWorkflowById(
                workflowId, userId, UserRole.TECH_SUPPORT
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(workflowId);
        assertThat(result.getTasks()).hasSize(2);

        verify(taskInstanceRepository).existsByWorkflowInstanceIdAndAssignedUserId(workflowId, userId);
    }

    @Test
    @DisplayName("getWorkflowById - Non-admin without access throws ForbiddenException")
    void getWorkflowById_UnauthorizedNonAdmin_ThrowsForbiddenException() {
        // Arrange
        when(workflowInstanceRepository.findById(workflowId))
                .thenReturn(Optional.of(workflowInstance));

        when(taskInstanceRepository.existsByWorkflowInstanceIdAndAssignedUserId(workflowId, userId))
                .thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> workflowService.getWorkflowById(
                workflowId, userId, UserRole.TECH_SUPPORT
        ))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("not authorized");

        verify(taskInstanceRepository).existsByWorkflowInstanceIdAndAssignedUserId(workflowId, userId);
        verify(taskInstanceRepository, never()).findByWorkflowInstanceIdOrderBySequenceOrder(any());
    }

    @Test
    @DisplayName("getWorkflowById - Throws ResourceNotFoundException when workflow not found")
    void getWorkflowById_WorkflowNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(workflowInstanceRepository.findById(workflowId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> workflowService.getWorkflowById(
                workflowId, userId, UserRole.HR_ADMIN
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");

        verify(workflowInstanceRepository).findById(workflowId);
        verify(taskInstanceRepository, never()).findByWorkflowInstanceIdOrderBySequenceOrder(any());
    }

    @Test
    @DisplayName("getWorkflowById - Should map all workflow fields correctly")
    void getWorkflowById_MapsAllFieldsCorrectly() {
        // Arrange
        workflowInstance.setCompletedAt(LocalDateTime.now());

        when(workflowInstanceRepository.findById(workflowId))
                .thenReturn(Optional.of(workflowInstance));

        when(taskInstanceRepository.findByWorkflowInstanceIdOrderBySequenceOrder(workflowId))
                .thenReturn(Collections.emptyList());

        when(workflowStateHistoryRepository.findByWorkflowInstanceIdOrderByChangedAtDesc(workflowId))
                .thenReturn(Collections.emptyList());

        // Act
        WorkflowDetailResponse result = workflowService.getWorkflowById(
                workflowId, userId, UserRole.HR_ADMIN
        );

        // Assert
        assertThat(result.getId()).isEqualTo(workflowInstance.getId());
        assertThat(result.getEmployeeName()).isEqualTo(workflowInstance.getEmployeeName());
        assertThat(result.getEmployeeEmail()).isEqualTo(workflowInstance.getEmployeeEmail());
        assertThat(result.getEmployeeRole()).isEqualTo(workflowInstance.getEmployeeRole());
        assertThat(result.getWorkflowType()).isEqualTo(workflowInstance.getWorkflowType());
        assertThat(result.getStatus()).isEqualTo(workflowInstance.getStatus());
        assertThat(result.getInitiatedAt()).isEqualTo(workflowInstance.getInitiatedAt());
        assertThat(result.getCompletedAt()).isEqualTo(workflowInstance.getCompletedAt());
        assertThat(result.getInitiatedBy()).isEqualTo(workflowInstance.getInitiatedBy());
        assertThat(result.getCustomFieldValues()).isEqualTo(workflowInstance.getCustomFieldValues());
    }

    @Test
    @DisplayName("getWorkflowById - Should include task details with user names")
    void getWorkflowById_IncludesTaskDetailsWithUserNames() {
        // Arrange
        when(workflowInstanceRepository.findById(workflowId))
                .thenReturn(Optional.of(workflowInstance));

        UUID assignedUserId = UUID.randomUUID();
        TaskInstance task = new TaskInstance();
        task.setId(UUID.randomUUID());
        task.setTaskName("Setup laptop");
        task.setStatus(TaskStatus.COMPLETED);
        task.setAssignedUserId(assignedUserId);
        task.setAssignedRole(UserRole.TECH_SUPPORT);
        task.setIsVisible(true);
        task.setCompletedBy(assignedUserId);

        when(taskInstanceRepository.findByWorkflowInstanceIdOrderBySequenceOrder(workflowId))
                .thenReturn(List.of(task));

        when(workflowStateHistoryRepository.findByWorkflowInstanceIdOrderByChangedAtDesc(workflowId))
                .thenReturn(Collections.emptyList());

        User assignedUser = new User();
        assignedUser.setId(assignedUserId);
        assignedUser.setUsername("bob.it");
        assignedUser.setEmail("bob.it@company.com");

        when(userRepository.findById(assignedUserId))
                .thenReturn(Optional.of(assignedUser));

        // Act
        WorkflowDetailResponse result = workflowService.getWorkflowById(
                workflowId, userId, UserRole.HR_ADMIN
        );

        // Assert
        assertThat(result.getTasks()).hasSize(1);
        TaskInstanceSummary taskSummary = result.getTasks().get(0);
        assertThat(taskSummary.getTaskName()).isEqualTo("Setup laptop");
        assertThat(taskSummary.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(taskSummary.getAssignedUserName()).isEqualTo("bob.it");
    }

    @Test
    @DisplayName("getWorkflowById - Should include state history with user names")
    void getWorkflowById_IncludesStateHistoryWithUserNames() {
        // Arrange
        when(workflowInstanceRepository.findById(workflowId))
                .thenReturn(Optional.of(workflowInstance));

        when(taskInstanceRepository.findByWorkflowInstanceIdOrderBySequenceOrder(workflowId))
                .thenReturn(Collections.emptyList());

        UUID changedByUserId = UUID.randomUUID();
        WorkflowStateHistory history = new WorkflowStateHistory();
        history.setId(UUID.randomUUID());
        history.setPreviousStatus(WorkflowStatus.INITIATED);
        history.setNewStatus(WorkflowStatus.IN_PROGRESS);
        history.setChangedBy(changedByUserId);
        history.setChangedAt(LocalDateTime.now());
        history.setNotes("Tasks assigned");

        when(workflowStateHistoryRepository.findByWorkflowInstanceIdOrderByChangedAtDesc(workflowId))
                .thenReturn(List.of(history));

        User changedByUser = new User();
        changedByUser.setId(changedByUserId);
        changedByUser.setUsername("alice.hr");

        when(userRepository.findById(changedByUserId))
                .thenReturn(Optional.of(changedByUser));

        // Act
        WorkflowDetailResponse result = workflowService.getWorkflowById(
                workflowId, userId, UserRole.HR_ADMIN
        );

        // Assert
        assertThat(result.getStateHistory()).hasSize(1);
        WorkflowStateHistoryEntry historyEntry = result.getStateHistory().get(0);
        assertThat(historyEntry.getPreviousStatus()).isEqualTo(WorkflowStatus.INITIATED);
        assertThat(historyEntry.getNewStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);
        assertThat(historyEntry.getChangedByName()).isEqualTo("alice.hr");
        assertThat(historyEntry.getNotes()).isEqualTo("Tasks assigned");
    }

    // ========== Helper Methods ==========

    private List<TaskInstance> createMockTasks(int count) {
        List<TaskInstance> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TaskInstance task = new TaskInstance();
            task.setId(UUID.randomUUID());
            task.setTaskName("Task " + (i + 1));
            tasks.add(task);
        }
        return tasks;
    }

    /**
     * Creates a list of mock tasks with specified number completed.
     * @param totalCount Total number of tasks to create
     * @param completedCount Number of tasks that should have COMPLETED status
     * @return List of TaskInstance objects
     */
    private List<TaskInstance> createMockTasksWithStatus(int totalCount, int completedCount) {
        List<TaskInstance> tasks = new ArrayList<>();
        for (int i = 0; i < totalCount; i++) {
            TaskInstance task = new TaskInstance();
            task.setId(UUID.randomUUID());
            task.setTaskName("Task " + (i + 1));
            task.setStatus(i < completedCount ? TaskStatus.COMPLETED : TaskStatus.IN_PROGRESS);
            tasks.add(task);
        }
        return tasks;
    }

    private List<TaskInstance> createMockTaskInstances(int count) {
        List<TaskInstance> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TaskInstance task = new TaskInstance();
            task.setId(UUID.randomUUID());
            task.setTaskName("Task " + (i + 1));
            task.setStatus(TaskStatus.IN_PROGRESS);
            task.setAssignedUserId(UUID.randomUUID());
            task.setAssignedRole(UserRole.TECH_SUPPORT);
            task.setIsVisible(true);
            tasks.add(task);
        }
        return tasks;
    }

    private List<WorkflowStateHistory> createMockStateHistory(int count) {
        List<WorkflowStateHistory> history = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            WorkflowStateHistory entry = new WorkflowStateHistory();
            entry.setId(UUID.randomUUID());
            entry.setPreviousStatus(WorkflowStatus.INITIATED);
            entry.setNewStatus(WorkflowStatus.IN_PROGRESS);
            entry.setChangedBy(userId);
            entry.setChangedAt(LocalDateTime.now().minusHours(i));
            history.add(entry);
        }
        return history;
    }
}
