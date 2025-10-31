package com.magnab.employeelifecycle.service;

import com.magnab.employeelifecycle.dto.request.EmployeeDetails;
import com.magnab.employeelifecycle.dto.response.TaskAssignmentResult;
import com.magnab.employeelifecycle.dto.response.WorkflowCreationResult;
import com.magnab.employeelifecycle.entity.*;
import com.magnab.employeelifecycle.enums.TaskStatus;
import com.magnab.employeelifecycle.enums.UserRole;
import com.magnab.employeelifecycle.enums.WorkflowStatus;
import com.magnab.employeelifecycle.enums.WorkflowType;
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

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyList;
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

    @Nested
    @DisplayName("Story 3.3: Task Assignment & Routing Tests")
    class TaskAssignmentTests {

        private UUID workflowInstanceId;
        private WorkflowInstance mockWorkflowInstance;
        private List<TaskInstance> mockTaskInstances;
        private List<TemplateTask> mockAssignmentTemplateTasks;
        private User hrUser1;
        private User hrUser2;

        @BeforeEach
        void setupAssignmentMocks() {
            workflowInstanceId = UUID.randomUUID();

            // Setup mock workflow instance
            mockWorkflowInstance = new WorkflowInstance();
            mockWorkflowInstance.setId(workflowInstanceId);
            mockWorkflowInstance.setTemplateId(templateId);
            mockWorkflowInstance.setStatus(WorkflowStatus.INITIATED);
            mockWorkflowInstance.setInitiatedBy(userId);

            // Setup HR users for load balancing tests
            hrUser1 = new User();
            hrUser1.setId(UUID.randomUUID());
            hrUser1.setEmail("hr1@example.com");
            hrUser1.setRole(UserRole.HR_ADMIN);
            hrUser1.setIsActive(true);

            hrUser2 = new User();
            hrUser2.setId(UUID.randomUUID());
            hrUser2.setEmail("hr2@example.com");
            hrUser2.setRole(UserRole.HR_ADMIN);
            hrUser2.setIsActive(true);

            // Setup template tasks with dependencies
            mockAssignmentTemplateTasks = createAssignmentTemplateTasks();
            mockTaskInstances = createMockTaskInstances();
        }

        private List<TemplateTask> createAssignmentTemplateTasks() {
            TemplateTask task1 = new TemplateTask();
            task1.setId(UUID.randomUUID());
            task1.setTemplateId(templateId);
            task1.setTaskName("Setup HR Account");
            task1.setAssignedRole(UserRole.HR_ADMIN);
            task1.setSequenceOrder(1);
            task1.setDependsOnTask(null); // No dependency

            TemplateTask task2 = new TemplateTask();
            task2.setId(UUID.randomUUID());
            task2.setTemplateId(templateId);
            task2.setTaskName("Assign Equipment");
            task2.setAssignedRole(UserRole.HR_ADMIN);
            task2.setSequenceOrder(2);
            task2.setDependsOnTask(task1); // Depends on task1

            TemplateTask task3 = new TemplateTask();
            task3.setId(UUID.randomUUID());
            task3.setTemplateId(templateId);
            task3.setTaskName("Setup Workstation");
            task3.setAssignedRole(UserRole.TECH_SUPPORT);
            task3.setSequenceOrder(3);
            task3.setDependsOnTask(task2); // Depends on task2

            return Arrays.asList(task1, task2, task3);
        }

        private List<TaskInstance> createMockTaskInstances() {
            List<TaskInstance> tasks = new ArrayList<>();

            // Task 1 - ready to assign (no dependency)
            TaskInstance task1Instance = new TaskInstance();
            task1Instance.setId(UUID.randomUUID());
            task1Instance.setWorkflowInstanceId(workflowInstanceId);
            task1Instance.setTemplateTaskId(mockAssignmentTemplateTasks.get(0).getId());
            task1Instance.setTaskName("Setup HR Account");
            task1Instance.setAssignedRole(UserRole.HR_ADMIN);
            task1Instance.setStatus(TaskStatus.NOT_STARTED);
            task1Instance.setIsVisible(true);
            tasks.add(task1Instance);

            // Task 2 - blocked by dependency
            TaskInstance task2Instance = new TaskInstance();
            task2Instance.setId(UUID.randomUUID());
            task2Instance.setWorkflowInstanceId(workflowInstanceId);
            task2Instance.setTemplateTaskId(mockAssignmentTemplateTasks.get(1).getId());
            task2Instance.setTaskName("Assign Equipment");
            task2Instance.setAssignedRole(UserRole.HR_ADMIN);
            task2Instance.setStatus(TaskStatus.NOT_STARTED);
            task2Instance.setIsVisible(true);
            tasks.add(task2Instance);

            // Task 3 - blocked by dependency
            TaskInstance task3Instance = new TaskInstance();
            task3Instance.setId(UUID.randomUUID());
            task3Instance.setWorkflowInstanceId(workflowInstanceId);
            task3Instance.setTemplateTaskId(mockAssignmentTemplateTasks.get(2).getId());
            task3Instance.setTaskName("Setup Workstation");
            task3Instance.setAssignedRole(UserRole.TECH_SUPPORT);
            task3Instance.setStatus(TaskStatus.NOT_STARTED);
            task3Instance.setIsVisible(true);
            tasks.add(task3Instance);

            return tasks;
        }

        @Test
        @DisplayName("AC1: Should throw ResourceNotFoundException when workflow not found")
        void shouldThrowExceptionWhenWorkflowNotFound() {
            UUID invalidWorkflowId = UUID.randomUUID();
            when(workflowInstanceRepository.findById(invalidWorkflowId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    workflowService.assignTasksForWorkflow(invalidWorkflowId)
            )
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Workflow instance not found");

            verify(workflowInstanceRepository).findById(invalidWorkflowId);
        }

        @Test
        @DisplayName("AC2: Should assign task with correct status and due date")
        void shouldAssignTaskWithCorrectStatusAndDueDate() {
            // Setup
            when(workflowInstanceRepository.findById(workflowInstanceId))
                    .thenReturn(Optional.of(mockWorkflowInstance));
            when(taskInstanceRepository.findByWorkflowInstanceIdOrderBySequenceOrder(workflowInstanceId))
                    .thenReturn(mockTaskInstances);
            when(templateTaskRepository.findByTemplateIdOrderBySequenceOrder(templateId))
                    .thenReturn(mockAssignmentTemplateTasks);
            when(userRepository.findByRoleAndIsActive(UserRole.HR_ADMIN, true))
                    .thenReturn(Collections.singletonList(hrUser1));
            when(taskInstanceRepository.countByAssignedUserIdAndStatus(hrUser1.getId(), TaskStatus.IN_PROGRESS))
                    .thenReturn(0L);
            when(taskInstanceRepository.saveAll(anyList())).thenReturn(mockTaskInstances);
            when(workflowInstanceRepository.save(any(WorkflowInstance.class)))
                    .thenReturn(mockWorkflowInstance);
            when(workflowStateHistoryRepository.save(any(WorkflowStateHistory.class)))
                    .thenReturn(new WorkflowStateHistory());

            // Execute
            LocalDateTime beforeAssignment = LocalDateTime.now();
            List<TaskAssignmentResult> results = workflowService.assignTasksForWorkflow(workflowInstanceId);
            LocalDateTime afterAssignment = LocalDateTime.now().plusDays(2).plusMinutes(1);

            // Verify: Only task 1 assigned (no dependencies)
            assertThat(results).hasSize(1);
            TaskAssignmentResult result = results.get(0);
            assertThat(result.getTaskInstanceId()).isEqualTo(mockTaskInstances.get(0).getId());
            assertThat(result.getAssignedUserId()).isEqualTo(hrUser1.getId());
            assertThat(result.getAssignedUserEmail()).isEqualTo("hr1@example.com");
            assertThat(result.getTaskName()).isEqualTo("Setup HR Account");
            assertThat(result.getDueDate()).isAfterOrEqualTo(beforeAssignment.plusDays(2));
            assertThat(result.getDueDate()).isBefore(afterAssignment);

            // Verify task status updated
            TaskInstance assignedTask = mockTaskInstances.get(0);
            assertThat(assignedTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(assignedTask.getAssignedUserId()).isEqualTo(hrUser1.getId());
        }

        @Test
        @DisplayName("AC3: Should implement load balancing - assign to user with fewest IN_PROGRESS tasks")
        void shouldAssignToUserWithFewestInProgressTasks() {
            // Setup: hrUser1 has 3 IN_PROGRESS tasks, hrUser2 has 1 IN_PROGRESS task
            when(workflowInstanceRepository.findById(workflowInstanceId))
                    .thenReturn(Optional.of(mockWorkflowInstance));
            when(taskInstanceRepository.findByWorkflowInstanceIdOrderBySequenceOrder(workflowInstanceId))
                    .thenReturn(mockTaskInstances);
            when(templateTaskRepository.findByTemplateIdOrderBySequenceOrder(templateId))
                    .thenReturn(mockAssignmentTemplateTasks);
            when(userRepository.findByRoleAndIsActive(UserRole.HR_ADMIN, true))
                    .thenReturn(Arrays.asList(hrUser1, hrUser2));
            when(taskInstanceRepository.countByAssignedUserIdAndStatus(hrUser1.getId(), TaskStatus.IN_PROGRESS))
                    .thenReturn(3L);
            when(taskInstanceRepository.countByAssignedUserIdAndStatus(hrUser2.getId(), TaskStatus.IN_PROGRESS))
                    .thenReturn(1L);
            when(taskInstanceRepository.saveAll(anyList())).thenReturn(mockTaskInstances);
            when(workflowInstanceRepository.save(any(WorkflowInstance.class)))
                    .thenReturn(mockWorkflowInstance);
            when(workflowStateHistoryRepository.save(any(WorkflowStateHistory.class)))
                    .thenReturn(new WorkflowStateHistory());

            // Execute
            List<TaskAssignmentResult> results = workflowService.assignTasksForWorkflow(workflowInstanceId);

            // Verify: Task assigned to hrUser2 (fewer IN_PROGRESS tasks)
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getAssignedUserId()).isEqualTo(hrUser2.getId());
            assertThat(results.get(0).getAssignedUserEmail()).isEqualTo("hr2@example.com");
        }

        @Test
        @DisplayName("AC4: Should only assign tasks with satisfied dependencies")
        void shouldOnlyAssignTasksWithSatisfiedDependencies() {
            // Setup: task1 is COMPLETED, task2 should be assignable
            mockTaskInstances.get(0).setStatus(TaskStatus.COMPLETED);
            mockTaskInstances.get(0).setAssignedUserId(hrUser1.getId());

            when(workflowInstanceRepository.findById(workflowInstanceId))
                    .thenReturn(Optional.of(mockWorkflowInstance));
            when(taskInstanceRepository.findByWorkflowInstanceIdOrderBySequenceOrder(workflowInstanceId))
                    .thenReturn(mockTaskInstances);
            when(templateTaskRepository.findByTemplateIdOrderBySequenceOrder(templateId))
                    .thenReturn(mockAssignmentTemplateTasks);
            when(userRepository.findByRoleAndIsActive(UserRole.HR_ADMIN, true))
                    .thenReturn(Collections.singletonList(hrUser1));
            when(taskInstanceRepository.countByAssignedUserIdAndStatus(hrUser1.getId(), TaskStatus.IN_PROGRESS))
                    .thenReturn(0L);
            when(taskInstanceRepository.saveAll(anyList())).thenReturn(mockTaskInstances);

            // Execute
            List<TaskAssignmentResult> results = workflowService.assignTasksForWorkflow(workflowInstanceId);

            // Verify: Only task2 assigned (task1 completed, task3 still blocked)
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTaskName()).isEqualTo("Assign Equipment");
        }

        @Test
        @DisplayName("AC5: Should be idempotent - calling multiple times doesn't cause errors")
        void shouldBeIdempotent() {
            // Setup: task1 already assigned
            mockTaskInstances.get(0).setAssignedUserId(hrUser1.getId());
            mockTaskInstances.get(0).setStatus(TaskStatus.IN_PROGRESS);

            when(workflowInstanceRepository.findById(workflowInstanceId))
                    .thenReturn(Optional.of(mockWorkflowInstance));
            when(taskInstanceRepository.findByWorkflowInstanceIdOrderBySequenceOrder(workflowInstanceId))
                    .thenReturn(mockTaskInstances);
            when(templateTaskRepository.findByTemplateIdOrderBySequenceOrder(templateId))
                    .thenReturn(mockAssignmentTemplateTasks);

            // Execute
            List<TaskAssignmentResult> results = workflowService.assignTasksForWorkflow(workflowInstanceId);

            // Verify: No tasks assigned (all are already assigned or blocked)
            assertThat(results).isEmpty();
            verify(taskInstanceRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("AC6: Should handle case when no active users available for role")
        void shouldHandleNoActiveUsersForRole() {
            // Setup: No active HR users
            when(workflowInstanceRepository.findById(workflowInstanceId))
                    .thenReturn(Optional.of(mockWorkflowInstance));
            when(taskInstanceRepository.findByWorkflowInstanceIdOrderBySequenceOrder(workflowInstanceId))
                    .thenReturn(mockTaskInstances);
            when(templateTaskRepository.findByTemplateIdOrderBySequenceOrder(templateId))
                    .thenReturn(mockAssignmentTemplateTasks);
            when(userRepository.findByRoleAndIsActive(UserRole.HR_ADMIN, true))
                    .thenReturn(Collections.emptyList());

            // Execute
            List<TaskAssignmentResult> results = workflowService.assignTasksForWorkflow(workflowInstanceId);

            // Verify: No tasks assigned, but no exception thrown
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("AC7: Should update workflow status to IN_PROGRESS on first assignment")
        void shouldUpdateWorkflowStatusOnFirstAssignment() {
            // Setup
            when(workflowInstanceRepository.findById(workflowInstanceId))
                    .thenReturn(Optional.of(mockWorkflowInstance));
            when(taskInstanceRepository.findByWorkflowInstanceIdOrderBySequenceOrder(workflowInstanceId))
                    .thenReturn(mockTaskInstances);
            when(templateTaskRepository.findByTemplateIdOrderBySequenceOrder(templateId))
                    .thenReturn(mockAssignmentTemplateTasks);
            when(userRepository.findByRoleAndIsActive(UserRole.HR_ADMIN, true))
                    .thenReturn(Collections.singletonList(hrUser1));
            when(taskInstanceRepository.countByAssignedUserIdAndStatus(hrUser1.getId(), TaskStatus.IN_PROGRESS))
                    .thenReturn(0L);
            when(taskInstanceRepository.saveAll(anyList())).thenReturn(mockTaskInstances);
            when(workflowInstanceRepository.save(any(WorkflowInstance.class)))
                    .thenReturn(mockWorkflowInstance);
            when(workflowStateHistoryRepository.save(any(WorkflowStateHistory.class)))
                    .thenReturn(new WorkflowStateHistory());

            // Execute
            List<TaskAssignmentResult> results = workflowService.assignTasksForWorkflow(workflowInstanceId);

            // Verify: Workflow status updated to IN_PROGRESS
            assertThat(results).hasSize(1);
            verify(workflowInstanceRepository).save(mockWorkflowInstance);
            assertThat(mockWorkflowInstance.getStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);

            // Verify state history created
            ArgumentCaptor<WorkflowStateHistory> historyCaptor = ArgumentCaptor.forClass(WorkflowStateHistory.class);
            verify(workflowStateHistoryRepository).save(historyCaptor.capture());
            WorkflowStateHistory history = historyCaptor.getValue();
            assertThat(history.getPreviousStatus()).isEqualTo(WorkflowStatus.INITIATED);
            assertThat(history.getNewStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);
            assertThat(history.getNotes()).contains("first task assignment");
        }

        @Test
        @DisplayName("AC8: Should not update workflow status if already has assignments")
        void shouldNotUpdateWorkflowStatusIfAlreadyHasAssignments() {
            // Setup: task1 already assigned
            mockTaskInstances.get(0).setAssignedUserId(hrUser1.getId());
            mockTaskInstances.get(0).setStatus(TaskStatus.COMPLETED);

            mockWorkflowInstance.setStatus(WorkflowStatus.IN_PROGRESS);

            when(workflowInstanceRepository.findById(workflowInstanceId))
                    .thenReturn(Optional.of(mockWorkflowInstance));
            when(taskInstanceRepository.findByWorkflowInstanceIdOrderBySequenceOrder(workflowInstanceId))
                    .thenReturn(mockTaskInstances);
            when(templateTaskRepository.findByTemplateIdOrderBySequenceOrder(templateId))
                    .thenReturn(mockAssignmentTemplateTasks);
            when(userRepository.findByRoleAndIsActive(UserRole.HR_ADMIN, true))
                    .thenReturn(Collections.singletonList(hrUser1));
            when(taskInstanceRepository.countByAssignedUserIdAndStatus(hrUser1.getId(), TaskStatus.IN_PROGRESS))
                    .thenReturn(0L);
            when(taskInstanceRepository.saveAll(anyList())).thenReturn(mockTaskInstances);

            // Execute
            List<TaskAssignmentResult> results = workflowService.assignTasksForWorkflow(workflowInstanceId);

            // Verify: Workflow status not updated (already had assignments)
            verify(workflowInstanceRepository, never()).save(any(WorkflowInstance.class));
            verify(workflowStateHistoryRepository, never()).save(any(WorkflowStateHistory.class));
        }

        @Test
        @DisplayName("AC9: Should only assign visible tasks")
        void shouldOnlyAssignVisibleTasks() {
            // Setup: task1 is not visible
            mockTaskInstances.get(0).setIsVisible(false);

            when(workflowInstanceRepository.findById(workflowInstanceId))
                    .thenReturn(Optional.of(mockWorkflowInstance));
            when(taskInstanceRepository.findByWorkflowInstanceIdOrderBySequenceOrder(workflowInstanceId))
                    .thenReturn(mockTaskInstances);
            when(templateTaskRepository.findByTemplateIdOrderBySequenceOrder(templateId))
                    .thenReturn(mockAssignmentTemplateTasks);

            // Execute
            List<TaskAssignmentResult> results = workflowService.assignTasksForWorkflow(workflowInstanceId);

            // Verify: No tasks assigned (task1 not visible, others blocked)
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("AC10: Should assign multiple tasks when dependencies satisfied")
        void shouldAssignMultipleTasksWhenDependenciesSatisfied() {
            // Setup: Modify template tasks to remove dependencies
            mockAssignmentTemplateTasks.get(1).setDependsOnTask(null);

            when(workflowInstanceRepository.findById(workflowInstanceId))
                    .thenReturn(Optional.of(mockWorkflowInstance));
            when(taskInstanceRepository.findByWorkflowInstanceIdOrderBySequenceOrder(workflowInstanceId))
                    .thenReturn(mockTaskInstances);
            when(templateTaskRepository.findByTemplateIdOrderBySequenceOrder(templateId))
                    .thenReturn(mockAssignmentTemplateTasks);
            when(userRepository.findByRoleAndIsActive(UserRole.HR_ADMIN, true))
                    .thenReturn(Collections.singletonList(hrUser1));
            when(taskInstanceRepository.countByAssignedUserIdAndStatus(hrUser1.getId(), TaskStatus.IN_PROGRESS))
                    .thenReturn(0L);
            when(taskInstanceRepository.saveAll(anyList())).thenReturn(mockTaskInstances);
            when(workflowInstanceRepository.save(any(WorkflowInstance.class)))
                    .thenReturn(mockWorkflowInstance);
            when(workflowStateHistoryRepository.save(any(WorkflowStateHistory.class)))
                    .thenReturn(new WorkflowStateHistory());

            // Execute
            List<TaskAssignmentResult> results = workflowService.assignTasksForWorkflow(workflowInstanceId);

            // Verify: First two tasks assigned (task1 and task2, both HR_ADMIN role, no dependencies)
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getTaskName()).isEqualTo("Setup HR Account");
            assertThat(results.get(1).getTaskName()).isEqualTo("Assign Equipment");
        }
    }
}
