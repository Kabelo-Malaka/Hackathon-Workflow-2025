package com.magnab.employeelifecycle.service;

import com.magnab.employeelifecycle.dto.request.EmployeeDetails;
import com.magnab.employeelifecycle.dto.response.WorkflowCreationResult;
import com.magnab.employeelifecycle.entity.*;
import com.magnab.employeelifecycle.enums.TaskStatus;
import com.magnab.employeelifecycle.enums.UserRole;
import com.magnab.employeelifecycle.enums.WorkflowStatus;
import com.magnab.employeelifecycle.exception.ResourceNotFoundException;
import com.magnab.employeelifecycle.exception.ValidationException;
import com.magnab.employeelifecycle.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WorkflowService.
 * Tests workflow instantiation logic with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowService Unit Tests")
class WorkflowServiceTest {

    @Mock
    private WorkflowInstanceRepository workflowInstanceRepository;

    @Mock
    private TaskInstanceRepository taskInstanceRepository;

    @Mock
    private WorkflowStateHistoryRepository workflowStateHistoryRepository;

    @Mock
    private WorkflowTemplateRepository workflowTemplateRepository;

    @Mock
    private TemplateTaskRepository templateTaskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WorkflowService workflowService;

    private UUID templateId;
    private UUID userId;
    private WorkflowTemplate mockTemplate;
    private User mockUser;
    private EmployeeDetails employeeDetails;
    private List<TemplateTask> mockTemplateTasks;

    @BeforeEach
    void setUp() {
        templateId = UUID.randomUUID();
        userId = UUID.randomUUID();

        // Setup mock template
        mockTemplate = new WorkflowTemplate();
        mockTemplate.setId(templateId);
        mockTemplate.setTemplateName("Onboarding Workflow");
        mockTemplate.setWorkflowType(com.magnab.employeelifecycle.enums.WorkflowType.ONBOARDING);
        mockTemplate.setIsActive(true);

        // Setup mock user
        mockUser = new User();
        mockUser.setId(userId);
        mockUser.setUsername("admin");
        mockUser.setRole(UserRole.ADMINISTRATOR);

        // Setup employee details
        employeeDetails = new EmployeeDetails();
        employeeDetails.setEmployeeName("John Doe");
        employeeDetails.setEmployeeEmail("john.doe@example.com");
        employeeDetails.setEmployeeRole("Software Engineer");

        // Setup mock template tasks
        mockTemplateTasks = createMockTemplateTasks();
    }

    private List<TemplateTask> createMockTemplateTasks() {
        UUID templateId = UUID.randomUUID();

        TemplateTask task1 = new TemplateTask();
        task1.setId(UUID.randomUUID());
        task1.setTemplateId(templateId);
        task1.setTaskName("Task 1");
        task1.setAssignedRole(UserRole.HR_ADMIN);
        task1.setSequenceOrder(1);

        TemplateTask task2 = new TemplateTask();
        task2.setId(UUID.randomUUID());
        task2.setTemplateId(templateId);
        task2.setTaskName("Task 2");
        task2.setAssignedRole(UserRole.LINE_MANAGER);
        task2.setSequenceOrder(2);

        TemplateTask task3 = new TemplateTask();
        task3.setId(UUID.randomUUID());
        task3.setTemplateId(templateId);
        task3.setTaskName("Task 3");
        task3.setAssignedRole(UserRole.TECH_SUPPORT);
        task3.setSequenceOrder(3);

        return Arrays.asList(task1, task2, task3);
    }

    @Nested
    @DisplayName("AC1: Template Validation Tests")
    class TemplateValidationTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when template not found")
        void shouldThrowExceptionWhenTemplateNotFound() {
            when(workflowTemplateRepository.findById(templateId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    workflowService.createWorkflowInstance(templateId, employeeDetails, null, userId)
            )
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Workflow template not found");

            verify(workflowTemplateRepository).findById(templateId);
            verifyNoInteractions(workflowInstanceRepository, taskInstanceRepository);
        }

        @Test
        @DisplayName("Should throw ValidationException when template is inactive")
        void shouldThrowExceptionWhenTemplateInactive() {
            mockTemplate.setIsActive(false);
            when(workflowTemplateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));

            assertThatThrownBy(() ->
                    workflowService.createWorkflowInstance(templateId, employeeDetails, null, userId)
            )
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Cannot instantiate inactive workflow template");

            verify(workflowTemplateRepository).findById(templateId);
            verifyNoInteractions(workflowInstanceRepository, taskInstanceRepository);
        }
    }

    @Nested
    @DisplayName("AC2: User Validation Tests")
    class UserValidationTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when initiating user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(workflowTemplateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    workflowService.createWorkflowInstance(templateId, employeeDetails, null, userId)
            )
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");

            verify(userRepository).findById(userId);
            verifyNoInteractions(workflowInstanceRepository, taskInstanceRepository);
        }
    }

    @Nested
    @DisplayName("AC3: Employee Details Validation Tests")
    class EmployeeDetailsValidationTests {

        @Test
        @DisplayName("Should throw ValidationException when employee details are null")
        void shouldThrowExceptionWhenEmployeeDetailsNull() {
            // No need to mock template or user since validation happens first
            assertThatThrownBy(() ->
                    workflowService.createWorkflowInstance(templateId, null, null, userId)
            )
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Employee details are required");

            verifyNoInteractions(workflowTemplateRepository, userRepository, workflowInstanceRepository, taskInstanceRepository);
        }
    }

    @Nested
    @DisplayName("AC4-9: Workflow Creation Success Tests")
    class WorkflowCreationSuccessTests {

        @BeforeEach
        void setupMocks() {
            when(workflowTemplateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));
            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
            when(templateTaskRepository.findByTemplateIdOrderBySequenceOrder(templateId))
                    .thenReturn(mockTemplateTasks);
        }

        @Test
        @DisplayName("AC4: Should create workflow instance with INITIATED status")
        void shouldCreateWorkflowInstanceWithInitiatedStatus() {
            // Setup: Mock repository to return saved instance with ID
            WorkflowInstance savedInstance = new WorkflowInstance();
            savedInstance.setId(UUID.randomUUID());
            savedInstance.setStatus(WorkflowStatus.INITIATED);
            when(workflowInstanceRepository.save(any(WorkflowInstance.class))).thenReturn(savedInstance);
            when(taskInstanceRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
            when(workflowStateHistoryRepository.save(any(WorkflowStateHistory.class)))
                    .thenReturn(new WorkflowStateHistory());

            // Execute
            workflowService.createWorkflowInstance(templateId, employeeDetails, null, userId);

            // Verify: Workflow instance created with correct properties
            ArgumentCaptor<WorkflowInstance> instanceCaptor = ArgumentCaptor.forClass(WorkflowInstance.class);
            verify(workflowInstanceRepository).save(instanceCaptor.capture());

            WorkflowInstance capturedInstance = instanceCaptor.getValue();
            assertThat(capturedInstance.getTemplateId()).isEqualTo(templateId);
            assertThat(capturedInstance.getEmployeeName()).isEqualTo("John Doe");
            assertThat(capturedInstance.getEmployeeEmail()).isEqualTo("john.doe@example.com");
            assertThat(capturedInstance.getEmployeeRole()).isEqualTo("Software Engineer");
            assertThat(capturedInstance.getWorkflowType()).isNotNull();
            assertThat(capturedInstance.getStatus()).isEqualTo(WorkflowStatus.INITIATED);
            assertThat(capturedInstance.getInitiatedBy()).isEqualTo(userId);
            assertThat(capturedInstance.getInitiatedAt()).isNotNull();
        }

        @Test
        @DisplayName("AC5: Should create task instances from template tasks")
        void shouldCreateTaskInstancesFromTemplate() {
            // Setup
            WorkflowInstance savedInstance = new WorkflowInstance();
            savedInstance.setId(UUID.randomUUID());
            when(workflowInstanceRepository.save(any(WorkflowInstance.class))).thenReturn(savedInstance);
            when(taskInstanceRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
            when(workflowStateHistoryRepository.save(any(WorkflowStateHistory.class)))
                    .thenReturn(new WorkflowStateHistory());

            // Execute
            workflowService.createWorkflowInstance(templateId, employeeDetails, null, userId);

            // Verify: Task instances created
            ArgumentCaptor<List<TaskInstance>> taskCaptor = ArgumentCaptor.forClass(List.class);
            verify(taskInstanceRepository).saveAll(taskCaptor.capture());

            List<TaskInstance> capturedTasks = taskCaptor.getValue();
            assertThat(capturedTasks).hasSize(3);

            // Verify first task
            TaskInstance task1 = capturedTasks.get(0);
            assertThat(task1.getWorkflowInstanceId()).isEqualTo(savedInstance.getId());
            assertThat(task1.getTemplateTaskId()).isEqualTo(mockTemplateTasks.get(0).getId());
            assertThat(task1.getTaskName()).isEqualTo("Task 1");
            assertThat(task1.getAssignedRole()).isEqualTo(UserRole.HR_ADMIN);
            assertThat(task1.getStatus()).isEqualTo(TaskStatus.NOT_STARTED);
            assertThat(task1.getIsVisible()).isTrue();

            // Verify second task
            TaskInstance task2 = capturedTasks.get(1);
            assertThat(task2.getTaskName()).isEqualTo("Task 2");
            assertThat(task2.getAssignedRole()).isEqualTo(UserRole.LINE_MANAGER);
            assertThat(task2.getIsVisible()).isTrue();

            // Verify third task
            TaskInstance task3 = capturedTasks.get(2);
            assertThat(task3.getTaskName()).isEqualTo("Task 3");
            assertThat(task3.getAssignedRole()).isEqualTo(UserRole.TECH_SUPPORT);
            assertThat(task3.getIsVisible()).isTrue();
        }

        @Test
        @DisplayName("AC6: Should create initial workflow state history")
        void shouldCreateInitialStateHistory() {
            // Setup
            WorkflowInstance savedInstance = new WorkflowInstance();
            savedInstance.setId(UUID.randomUUID());
            when(workflowInstanceRepository.save(any(WorkflowInstance.class))).thenReturn(savedInstance);
            when(taskInstanceRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
            when(workflowStateHistoryRepository.save(any(WorkflowStateHistory.class)))
                    .thenReturn(new WorkflowStateHistory());

            // Execute
            workflowService.createWorkflowInstance(templateId, employeeDetails, null, userId);

            // Verify: State history created
            ArgumentCaptor<WorkflowStateHistory> historyCaptor = ArgumentCaptor.forClass(WorkflowStateHistory.class);
            verify(workflowStateHistoryRepository).save(historyCaptor.capture());

            WorkflowStateHistory capturedHistory = historyCaptor.getValue();
            assertThat(capturedHistory.getWorkflowInstanceId()).isEqualTo(savedInstance.getId());
            assertThat(capturedHistory.getPreviousStatus()).isEqualTo(WorkflowStatus.INITIATED);
            assertThat(capturedHistory.getNewStatus()).isEqualTo(WorkflowStatus.INITIATED);
            assertThat(capturedHistory.getChangedBy()).isEqualTo(userId);
            assertThat(capturedHistory.getChangedAt()).isNotNull();
            assertThat(capturedHistory.getNotes()).isEqualTo("Workflow initiated");
        }

        @Test
        @DisplayName("AC7: Should return workflow creation summary with task counts")
        void shouldReturnCreationSummary() {
            // Setup
            WorkflowInstance savedInstance = new WorkflowInstance();
            UUID workflowInstanceId = UUID.randomUUID();
            savedInstance.setId(workflowInstanceId);
            when(workflowInstanceRepository.save(any(WorkflowInstance.class))).thenReturn(savedInstance);
            when(taskInstanceRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
            when(workflowStateHistoryRepository.save(any(WorkflowStateHistory.class)))
                    .thenReturn(new WorkflowStateHistory());

            // Execute
            WorkflowCreationResult result = workflowService.createWorkflowInstance(
                    templateId, employeeDetails, null, userId);

            // Verify: Result contains correct summary
            assertThat(result).isNotNull();
            assertThat(result.getWorkflowInstanceId()).isEqualTo(workflowInstanceId);
            assertThat(result.getTotalTasks()).isEqualTo(3);
            assertThat(result.getImmediateTasksCount()).isEqualTo(3); // All tasks visible in MVP
        }

        @Test
        @DisplayName("AC8-9: Should handle custom field values")
        void shouldHandleCustomFieldValues() {
            // Setup: Custom field values
            Map<String, Object> customFieldValues = new HashMap<>();
            customFieldValues.put("needsLaptop", true);
            customFieldValues.put("department", "Engineering");

            WorkflowInstance savedInstance = new WorkflowInstance();
            savedInstance.setId(UUID.randomUUID());
            when(workflowInstanceRepository.save(any(WorkflowInstance.class))).thenReturn(savedInstance);
            when(taskInstanceRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
            when(workflowStateHistoryRepository.save(any(WorkflowStateHistory.class)))
                    .thenReturn(new WorkflowStateHistory());

            // Execute
            WorkflowCreationResult result = workflowService.createWorkflowInstance(
                    templateId, employeeDetails, customFieldValues, userId);

            // Verify: Custom fields stored in workflow instance
            ArgumentCaptor<WorkflowInstance> instanceCaptor = ArgumentCaptor.forClass(WorkflowInstance.class);
            verify(workflowInstanceRepository).save(instanceCaptor.capture());

            WorkflowInstance capturedInstance = instanceCaptor.getValue();
            assertThat(capturedInstance.getCustomFieldValues()).containsEntry("needsLaptop", true);
            assertThat(capturedInstance.getCustomFieldValues()).containsEntry("department", "Engineering");

            // All tasks visible in MVP (no conditional logic yet)
            assertThat(result.getImmediateTasksCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("AC9: Should handle empty custom field values")
        void shouldHandleEmptyCustomFieldValues() {
            // Setup
            WorkflowInstance savedInstance = new WorkflowInstance();
            savedInstance.setId(UUID.randomUUID());
            when(workflowInstanceRepository.save(any(WorkflowInstance.class))).thenReturn(savedInstance);
            when(taskInstanceRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
            when(workflowStateHistoryRepository.save(any(WorkflowStateHistory.class)))
                    .thenReturn(new WorkflowStateHistory());

            // Execute: Pass null custom field values
            WorkflowCreationResult result = workflowService.createWorkflowInstance(
                    templateId, employeeDetails, null, userId);

            // Verify: Workflow created successfully with empty custom fields
            ArgumentCaptor<WorkflowInstance> instanceCaptor = ArgumentCaptor.forClass(WorkflowInstance.class);
            verify(workflowInstanceRepository).save(instanceCaptor.capture());

            WorkflowInstance capturedInstance = instanceCaptor.getValue();
            assertThat(capturedInstance.getCustomFieldValues()).isNotNull();
            assertThat(capturedInstance.getCustomFieldValues()).isEmpty();

            // All tasks visible in MVP
            assertThat(result.getImmediateTasksCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("AC10: Transaction Rollback Tests")
    class TransactionRollbackTests {

        @Test
        @DisplayName("Should rollback transaction when task instance creation fails")
        void shouldRollbackWhenTaskInstanceCreationFails() {
            // Setup: Mock workflow instance save succeeds, but task instance save fails
            when(workflowTemplateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));
            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
            when(templateTaskRepository.findByTemplateIdOrderBySequenceOrder(templateId))
                    .thenReturn(mockTemplateTasks);

            WorkflowInstance savedInstance = new WorkflowInstance();
            savedInstance.setId(UUID.randomUUID());
            when(workflowInstanceRepository.save(any(WorkflowInstance.class))).thenReturn(savedInstance);

            // Simulate database constraint violation
            when(taskInstanceRepository.saveAll(anyList()))
                    .thenThrow(new RuntimeException("Database constraint violation"));

            // Execute & Verify: Exception propagates (transaction will rollback due to @Transactional)
            assertThatThrownBy(() ->
                    workflowService.createWorkflowInstance(templateId, employeeDetails, null, userId)
            )
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database constraint violation");

            // State history should not be saved if task instance save fails
            verify(workflowStateHistoryRepository, never()).save(any(WorkflowStateHistory.class));
        }
    }
}
