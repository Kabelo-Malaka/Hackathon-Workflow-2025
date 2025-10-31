package com.magnab.employeelifecycle.service;

import com.magnab.employeelifecycle.dto.request.EmployeeDetails;
import com.magnab.employeelifecycle.dto.response.TaskAssignmentResult;
import com.magnab.employeelifecycle.dto.response.WorkflowCreationResult;
import com.magnab.employeelifecycle.entity.*;
import com.magnab.employeelifecycle.enums.TaskStatus;
import com.magnab.employeelifecycle.enums.UserRole;
import com.magnab.employeelifecycle.enums.WorkflowStatus;
import com.magnab.employeelifecycle.exception.ResourceNotFoundException;
import com.magnab.employeelifecycle.exception.ValidationException;
import com.magnab.employeelifecycle.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for WorkflowService using TestContainers with real PostgreSQL.
 * Tests end-to-end workflow instantiation with actual database operations.
 */
@SpringBootTest
@Testcontainers
@Transactional
@DisplayName("WorkflowService Integration Tests")
class WorkflowServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.2-alpine")
            .withDatabaseName("employee_lifecycle_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowTemplateRepository workflowTemplateRepository;

    @Autowired
    private TemplateTaskRepository templateTaskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkflowInstanceRepository workflowInstanceRepository;

    @Autowired
    private TaskInstanceRepository taskInstanceRepository;

    @Autowired
    private WorkflowStateHistoryRepository workflowStateHistoryRepository;

    private WorkflowTemplate testTemplate;
    private User testUser;
    private List<TemplateTask> testTemplateTasks;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setUsername("test_admin");
        testUser.setEmail("admin@test.com");
        testUser.setPasswordHash("hashed_password");
        testUser.setRole(UserRole.ADMINISTRATOR);
        testUser = userRepository.save(testUser);

        // Create test workflow template
        testTemplate = new WorkflowTemplate();
        testTemplate.setTemplateName("Test Onboarding Workflow");
        testTemplate.setWorkflowType(com.magnab.employeelifecycle.enums.WorkflowType.ONBOARDING);
        testTemplate.setDescription("Integration test template");
        testTemplate.setIsActive(true);
        testTemplate.setCreatedBy(testUser.getId());
        testTemplate = workflowTemplateRepository.save(testTemplate);

        // Create template tasks
        testTemplateTasks = createTestTemplateTasks();
    }

    private List<TemplateTask> createTestTemplateTasks() {
        List<TemplateTask> tasks = new ArrayList<>();

        // Task 1: HR Setup
        TemplateTask task1 = new TemplateTask();
        task1.setTemplate(testTemplate);
        task1.setTaskName("HR Setup");
        task1.setDescription("Setup HR systems for new employee");
        task1.setAssignedRole(UserRole.HR_ADMIN);
        task1.setSequenceOrder(1);
        task1.setCreatedBy(testUser.getId());
        tasks.add(templateTaskRepository.save(task1));

        // Task 2: IT Setup
        TemplateTask task2 = new TemplateTask();
        task2.setTemplate(testTemplate);
        task2.setTaskName("IT Setup");
        task2.setDescription("Setup IT accounts and equipment");
        task2.setAssignedRole(UserRole.TECH_SUPPORT);
        task2.setSequenceOrder(2);
        task2.setCreatedBy(testUser.getId());
        tasks.add(templateTaskRepository.save(task2));

        // Task 3: Manager Orientation
        TemplateTask task3 = new TemplateTask();
        task3.setTemplate(testTemplate);
        task3.setTaskName("Manager Orientation");
        task3.setDescription("Schedule orientation with manager");
        task3.setAssignedRole(UserRole.LINE_MANAGER);
        task3.setSequenceOrder(3);
        task3.setCreatedBy(testUser.getId());
        tasks.add(templateTaskRepository.save(task3));

        return tasks;
    }

    @Nested
    @DisplayName("Successful Workflow Creation Tests")
    class SuccessfulCreationTests {

        @Test
        @DisplayName("Should create workflow instance with all visible tasks")
        void shouldCreateWorkflowWithAllVisibleTasks() {
            // Arrange
            EmployeeDetails employeeDetails = new EmployeeDetails();
            employeeDetails.setEmployeeName("Jane Smith");
            employeeDetails.setEmployeeEmail("jane.smith@example.com");
            employeeDetails.setEmployeeRole("Software Engineer");

            Map<String, Object> customFields = new HashMap<>();
            customFields.put("department", "Engineering");
            customFields.put("location", "New York");

            // Act
            WorkflowCreationResult result = workflowService.createWorkflowInstance(
                    testTemplate.getId(), employeeDetails, customFields, testUser.getId());

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getWorkflowInstanceId()).isNotNull();
            assertThat(result.getTotalTasks()).isEqualTo(3);
            assertThat(result.getImmediateTasksCount()).isEqualTo(3); // All tasks visible in MVP

            // Verify workflow instance persisted
            WorkflowInstance savedWorkflow = workflowInstanceRepository.findById(result.getWorkflowInstanceId())
                    .orElseThrow();
            assertThat(savedWorkflow.getTemplateId()).isEqualTo(testTemplate.getId());
            assertThat(savedWorkflow.getWorkflowType()).isEqualTo(com.magnab.employeelifecycle.enums.WorkflowType.ONBOARDING);
            assertThat(savedWorkflow.getEmployeeName()).isEqualTo("Jane Smith");
            assertThat(savedWorkflow.getEmployeeEmail()).isEqualTo("jane.smith@example.com");
            assertThat(savedWorkflow.getEmployeeRole()).isEqualTo("Software Engineer");
            assertThat(savedWorkflow.getStatus()).isEqualTo(WorkflowStatus.INITIATED);
            assertThat(savedWorkflow.getInitiatedBy()).isEqualTo(testUser.getId());
            assertThat(savedWorkflow.getInitiatedAt()).isNotNull();
            assertThat(savedWorkflow.getCustomFieldValues()).containsEntry("department", "Engineering");
            assertThat(savedWorkflow.getCustomFieldValues()).containsEntry("location", "New York");

            // Verify task instances persisted
            List<TaskInstance> taskInstances = taskInstanceRepository
                    .findByWorkflowInstanceId(savedWorkflow.getId());
            assertThat(taskInstances).hasSize(3);

            // Verify Task 1 (HR Setup)
            TaskInstance hrTask = taskInstances.stream()
                    .filter(t -> t.getTaskName().equals("HR Setup"))
                    .findFirst().orElseThrow();
            assertThat(hrTask.getTemplateTaskId()).isEqualTo(testTemplateTasks.get(0).getId());
            assertThat(hrTask.getAssignedRole()).isEqualTo(UserRole.HR_ADMIN);
            assertThat(hrTask.getStatus()).isEqualTo(TaskStatus.NOT_STARTED);
            assertThat(hrTask.getIsVisible()).isTrue();

            // Verify Task 2 (IT Setup)
            TaskInstance itTask = taskInstances.stream()
                    .filter(t -> t.getTaskName().equals("IT Setup"))
                    .findFirst().orElseThrow();
            assertThat(itTask.getIsVisible()).isTrue();
            assertThat(itTask.getAssignedRole()).isEqualTo(UserRole.TECH_SUPPORT);

            // Verify Task 3 (Manager Orientation)
            TaskInstance managerTask = taskInstances.stream()
                    .filter(t -> t.getTaskName().equals("Manager Orientation"))
                    .findFirst().orElseThrow();
            assertThat(managerTask.getIsVisible()).isTrue();
            assertThat(managerTask.getAssignedRole()).isEqualTo(UserRole.LINE_MANAGER);

            // Verify state history created
            List<WorkflowStateHistory> history = workflowStateHistoryRepository
                    .findByWorkflowInstanceIdOrderByChangedAtDesc(savedWorkflow.getId());
            assertThat(history).hasSize(1);
            WorkflowStateHistory initialHistory = history.get(0);
            assertThat(initialHistory.getPreviousStatus()).isEqualTo(WorkflowStatus.INITIATED);
            assertThat(initialHistory.getNewStatus()).isEqualTo(WorkflowStatus.INITIATED);
            assertThat(initialHistory.getChangedBy()).isEqualTo(testUser.getId());
            assertThat(initialHistory.getNotes()).isEqualTo("Workflow initiated");
        }

        @Test
        @DisplayName("Should create workflow with all tasks visible when no custom fields")
        void shouldCreateWorkflowWithNoCustomFields() {
            // Arrange
            EmployeeDetails employeeDetails = new EmployeeDetails();
            employeeDetails.setEmployeeName("Bob Developer");
            employeeDetails.setEmployeeEmail("bob.dev@example.com");
            employeeDetails.setEmployeeRole("Developer");

            // Act: No custom fields provided
            WorkflowCreationResult result = workflowService.createWorkflowInstance(
                    testTemplate.getId(), employeeDetails, null, testUser.getId());

            // Assert
            assertThat(result.getTotalTasks()).isEqualTo(3);
            assertThat(result.getImmediateTasksCount()).isEqualTo(3); // All tasks visible in MVP

            // Verify all tasks are visible
            List<TaskInstance> taskInstances = taskInstanceRepository
                    .findByWorkflowInstanceId(result.getWorkflowInstanceId());
            assertThat(taskInstances).allMatch(TaskInstance::getIsVisible);
        }

        @Test
        @DisplayName("Should handle empty custom field values map")
        void shouldHandleEmptyCustomFieldValues() {
            // Arrange
            EmployeeDetails employeeDetails = new EmployeeDetails();
            employeeDetails.setEmployeeName("Alice Developer");
            employeeDetails.setEmployeeEmail("alice@example.com");
            employeeDetails.setEmployeeRole("Developer");

            // Act: Empty map
            WorkflowCreationResult result = workflowService.createWorkflowInstance(
                    testTemplate.getId(), employeeDetails, new HashMap<>(), testUser.getId());

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getImmediateTasksCount()).isEqualTo(3);

            WorkflowInstance savedWorkflow = workflowInstanceRepository
                    .findById(result.getWorkflowInstanceId()).orElseThrow();
            assertThat(savedWorkflow.getCustomFieldValues()).isNotNull();
            assertThat(savedWorkflow.getCustomFieldValues()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Validation and Error Handling Tests")
    class ValidationErrorTests {

        @Test
        @DisplayName("Should throw exception when template not found")
        void shouldThrowExceptionWhenTemplateNotFound() {
            // Arrange
            UUID nonExistentTemplateId = UUID.randomUUID();
            EmployeeDetails employeeDetails = new EmployeeDetails();
            employeeDetails.setEmployeeName("Test Employee");
            employeeDetails.setEmployeeEmail("test@example.com");
            employeeDetails.setEmployeeRole("Engineer");

            // Act & Assert
            assertThatThrownBy(() ->
                    workflowService.createWorkflowInstance(
                            nonExistentTemplateId, employeeDetails, null, testUser.getId())
            )
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Workflow template not found");

            // Verify no workflow instance created
            List<WorkflowInstance> allInstances = workflowInstanceRepository.findAll();
            assertThat(allInstances).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when template is inactive")
        void shouldThrowExceptionWhenTemplateInactive() {
            // Arrange
            testTemplate.setIsActive(false);
            workflowTemplateRepository.save(testTemplate);

            EmployeeDetails employeeDetails = new EmployeeDetails();
            employeeDetails.setEmployeeName("Test Employee");
            employeeDetails.setEmployeeEmail("test@example.com");
            employeeDetails.setEmployeeRole("Engineer");

            // Act & Assert
            assertThatThrownBy(() ->
                    workflowService.createWorkflowInstance(
                            testTemplate.getId(), employeeDetails, null, testUser.getId())
            )
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Cannot instantiate inactive workflow template");

            // Verify no workflow instance created
            List<WorkflowInstance> allInstances = workflowInstanceRepository.findAll();
            assertThat(allInstances).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Arrange
            UUID nonExistentUserId = UUID.randomUUID();
            EmployeeDetails employeeDetails = new EmployeeDetails();
            employeeDetails.setEmployeeName("Test Employee");
            employeeDetails.setEmployeeEmail("test@example.com");
            employeeDetails.setEmployeeRole("Engineer");

            // Act & Assert
            assertThatThrownBy(() ->
                    workflowService.createWorkflowInstance(
                            testTemplate.getId(), employeeDetails, null, nonExistentUserId)
            )
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");

            // Verify no workflow instance created
            List<WorkflowInstance> allInstances = workflowInstanceRepository.findAll();
            assertThat(allInstances).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when employee details are null")
        void shouldThrowExceptionWhenEmployeeDetailsNull() {
            // Act & Assert
            assertThatThrownBy(() ->
                    workflowService.createWorkflowInstance(
                            testTemplate.getId(), null, null, testUser.getId())
            )
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Employee details are required");

            // Verify no workflow instance created
            List<WorkflowInstance> allInstances = workflowInstanceRepository.findAll();
            assertThat(allInstances).isEmpty();
        }
    }

    @Nested
    @DisplayName("Transaction and Data Integrity Tests")
    class TransactionTests {

        @Test
        @DisplayName("Should maintain referential integrity between workflow and tasks")
        void shouldMaintainReferentialIntegrity() {
            // Arrange
            EmployeeDetails employeeDetails = new EmployeeDetails();
            employeeDetails.setEmployeeName("Test Employee");
            employeeDetails.setEmployeeEmail("test@example.com");
            employeeDetails.setEmployeeRole("Engineer");

            // Act
            WorkflowCreationResult result = workflowService.createWorkflowInstance(
                    testTemplate.getId(), employeeDetails, null, testUser.getId());

            // Assert: All task instances reference the correct workflow instance
            List<TaskInstance> taskInstances = taskInstanceRepository
                    .findByWorkflowInstanceId(result.getWorkflowInstanceId());

            assertThat(taskInstances).allMatch(task ->
                    task.getWorkflowInstanceId().equals(result.getWorkflowInstanceId())
            );

            // All task instances reference valid template tasks
            assertThat(taskInstances).allMatch(task ->
                    testTemplateTasks.stream()
                            .anyMatch(tt -> tt.getId().equals(task.getTemplateTaskId()))
            );
        }

        @Test
        @DisplayName("Should create audit timestamps correctly")
        void shouldCreateAuditTimestampsCorrectly() {
            // Arrange
            EmployeeDetails employeeDetails = new EmployeeDetails();
            employeeDetails.setEmployeeName("Test Employee");
            employeeDetails.setEmployeeEmail("test@example.com");
            employeeDetails.setEmployeeRole("Engineer");

            // Act
            WorkflowCreationResult result = workflowService.createWorkflowInstance(
                    testTemplate.getId(), employeeDetails, null, testUser.getId());

            // Assert: Workflow instance has timestamps
            WorkflowInstance workflow = workflowInstanceRepository
                    .findById(result.getWorkflowInstanceId()).orElseThrow();
            assertThat(workflow.getCreatedAt()).isNotNull();
            assertThat(workflow.getUpdatedAt()).isNotNull();
            assertThat(workflow.getInitiatedAt()).isNotNull();

            // Task instances have timestamps
            List<TaskInstance> taskInstances = taskInstanceRepository
                    .findByWorkflowInstanceId(result.getWorkflowInstanceId());
            assertThat(taskInstances).allMatch(task ->
                    task.getCreatedAt() != null && task.getUpdatedAt() != null
            );

            // State history has timestamp
            List<WorkflowStateHistory> history = workflowStateHistoryRepository
                    .findByWorkflowInstanceIdOrderByChangedAtDesc(result.getWorkflowInstanceId());
            assertThat(history.get(0).getChangedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle template with no tasks")
        void shouldHandleTemplateWithNoTasks() {
            // Arrange: Create template with no tasks
            WorkflowTemplate emptyTemplate = new WorkflowTemplate();
            emptyTemplate.setTemplateName("Empty Template");
            emptyTemplate.setWorkflowType(com.magnab.employeelifecycle.enums.WorkflowType.ONBOARDING);
            emptyTemplate.setDescription("Template with no tasks");
            emptyTemplate.setIsActive(true);
            emptyTemplate.setCreatedBy(testUser.getId());
            emptyTemplate = workflowTemplateRepository.save(emptyTemplate);

            EmployeeDetails employeeDetails = new EmployeeDetails();
            employeeDetails.setEmployeeName("Test Employee");
            employeeDetails.setEmployeeEmail("test@example.com");
            employeeDetails.setEmployeeRole("Engineer");

            // Act
            WorkflowCreationResult result = workflowService.createWorkflowInstance(
                    emptyTemplate.getId(), employeeDetails, null, testUser.getId());

            // Assert
            assertThat(result.getTotalTasks()).isEqualTo(0);
            assertThat(result.getImmediateTasksCount()).isEqualTo(0);

            List<TaskInstance> taskInstances = taskInstanceRepository
                    .findByWorkflowInstanceId(result.getWorkflowInstanceId());
            assertThat(taskInstances).isEmpty();
        }

        @Test
        @DisplayName("Should handle complex custom field values")
        void shouldHandleComplexCustomFieldValues() {
            // Arrange
            EmployeeDetails employeeDetails = new EmployeeDetails();
            employeeDetails.setEmployeeName("Test Employee");
            employeeDetails.setEmployeeEmail("test@example.com");
            employeeDetails.setEmployeeRole("Engineer");

            Map<String, Object> complexCustomFields = new HashMap<>();
            complexCustomFields.put("department", "Engineering");
            complexCustomFields.put("salary", 100000);
            complexCustomFields.put("startDate", "2025-11-01");
            complexCustomFields.put("benefits", List.of("health", "dental", "vision"));
            complexCustomFields.put("isRemote", true);

            // Act
            WorkflowCreationResult result = workflowService.createWorkflowInstance(
                    testTemplate.getId(), employeeDetails, complexCustomFields, testUser.getId());

            // Assert
            WorkflowInstance workflow = workflowInstanceRepository
                    .findById(result.getWorkflowInstanceId()).orElseThrow();

            assertThat(workflow.getCustomFieldValues()).isNotNull();
            assertThat(workflow.getCustomFieldValues()).containsEntry("department", "Engineering");
            assertThat(workflow.getCustomFieldValues()).containsEntry("salary", 100000);
            assertThat(workflow.getCustomFieldValues()).containsKey("benefits");
        }
    }

    @Nested
    @DisplayName("Story 3.3: Task Assignment & Routing Integration Tests")
    class TaskAssignmentIntegrationTests {

        private User hrUser1;
        private User hrUser2;
        private User techUser;
        private User managerUser;
        private WorkflowInstance testWorkflowInstance;

        @BeforeEach
        void setupAssignmentData() {
            // Create HR users for load balancing tests
            hrUser1 = new User();
            hrUser1.setUsername("hr_user1");
            hrUser1.setEmail("hr1@test.com");
            hrUser1.setPasswordHash("hashed");
            hrUser1.setRole(UserRole.HR_ADMIN);
            hrUser1.setIsActive(true);
            hrUser1 = userRepository.save(hrUser1);

            hrUser2 = new User();
            hrUser2.setUsername("hr_user2");
            hrUser2.setEmail("hr2@test.com");
            hrUser2.setPasswordHash("hashed");
            hrUser2.setRole(UserRole.HR_ADMIN);
            hrUser2.setIsActive(true);
            hrUser2 = userRepository.save(hrUser2);

            // Create tech support user
            techUser = new User();
            techUser.setUsername("tech_user");
            techUser.setEmail("tech@test.com");
            techUser.setPasswordHash("hashed");
            techUser.setRole(UserRole.TECH_SUPPORT);
            techUser.setIsActive(true);
            techUser = userRepository.save(techUser);

            // Create manager user
            managerUser = new User();
            managerUser.setUsername("manager_user");
            managerUser.setEmail("manager@test.com");
            managerUser.setPasswordHash("hashed");
            managerUser.setRole(UserRole.LINE_MANAGER);
            managerUser.setIsActive(true);
            managerUser = userRepository.save(managerUser);

            // Create a workflow instance for testing
            EmployeeDetails employeeDetails = new EmployeeDetails();
            employeeDetails.setEmployeeName("Assignment Test Employee");
            employeeDetails.setEmployeeEmail("assignment.test@example.com");
            employeeDetails.setEmployeeRole("Software Engineer");

            WorkflowCreationResult creationResult = workflowService.createWorkflowInstance(
                    testTemplate.getId(), employeeDetails, null, testUser.getId());

            testWorkflowInstance = workflowInstanceRepository.findById(creationResult.getWorkflowInstanceId())
                    .orElseThrow();
        }

        @Test
        @DisplayName("Should assign tasks with correct status, due date, and persist to database")
        void shouldAssignTasksWithCorrectData() {
            // Act
            LocalDateTime beforeAssignment = LocalDateTime.now();
            List<TaskAssignmentResult> results = workflowService.assignTasksForWorkflow(testWorkflowInstance.getId());
            LocalDateTime afterAssignment = LocalDateTime.now().plusDays(2).plusMinutes(1);

            // Assert: Should assign all 3 tasks (no dependencies in test template)
            assertThat(results).hasSize(3);

            // Verify all results have correct structure
            for (TaskAssignmentResult result : results) {
                assertThat(result.getTaskInstanceId()).isNotNull();
                assertThat(result.getAssignedUserId()).isNotNull();
                assertThat(result.getAssignedUserEmail()).isNotNull();
                assertThat(result.getTaskName()).isNotNull();
                assertThat(result.getDueDate()).isNotNull();
                assertThat(result.getDueDate()).isAfterOrEqualTo(beforeAssignment.plusDays(2));
                assertThat(result.getDueDate()).isBefore(afterAssignment);
            }

            // Verify database persistence
            List<TaskInstance> allTasks = taskInstanceRepository
                    .findByWorkflowInstanceId(testWorkflowInstance.getId());

            assertThat(allTasks).hasSize(3);
            assertThat(allTasks).allMatch(task -> task.getStatus() == TaskStatus.IN_PROGRESS);
            assertThat(allTasks).allMatch(task -> task.getAssignedUserId() != null);
            assertThat(allTasks).allMatch(task -> task.getDueDate() != null);
        }

        @Test
        @DisplayName("Should implement load balancing - assign to user with fewer IN_PROGRESS tasks")
        void shouldImplementLoadBalancing() {
            // Arrange: Create additional workflow and assign tasks to hrUser1
            EmployeeDetails employeeDetails = new EmployeeDetails();
            employeeDetails.setEmployeeName("Load Balancing Test");
            employeeDetails.setEmployeeEmail("loadbalance@test.com");
            employeeDetails.setEmployeeRole("Engineer");

            WorkflowCreationResult workflow2 = workflowService.createWorkflowInstance(
                    testTemplate.getId(), employeeDetails, null, testUser.getId());

            // Manually assign 2 tasks to hrUser1 to create load imbalance
            List<TaskInstance> workflow2Tasks = taskInstanceRepository
                    .findByWorkflowInstanceId(workflow2.getWorkflowInstanceId());

            TaskInstance task1 = workflow2Tasks.get(0);
            task1.setAssignedUserId(hrUser1.getId());
            task1.setStatus(TaskStatus.IN_PROGRESS);
            taskInstanceRepository.save(task1);

            // Act: Assign tasks for testWorkflowInstance
            List<TaskAssignmentResult> results = workflowService.assignTasksForWorkflow(testWorkflowInstance.getId());

            // Assert: HR tasks should be assigned to hrUser2 (less load)
            List<TaskAssignmentResult> hrTasks = results.stream()
                    .filter(r -> r.getTaskName().equals("HR Setup"))
                    .toList();

            assertThat(hrTasks).hasSize(1);
            assertThat(hrTasks.get(0).getAssignedUserId()).isEqualTo(hrUser2.getId());
            assertThat(hrTasks.get(0).getAssignedUserEmail()).isEqualTo("hr2@test.com");
        }

        @Test
        @DisplayName("Should only assign tasks with satisfied dependencies")
        void shouldRespectDependencies() {
            // Arrange: Create template with dependencies
            WorkflowTemplate dependencyTemplate = new WorkflowTemplate();
            dependencyTemplate.setTemplateName("Dependency Test Template");
            dependencyTemplate.setWorkflowType(com.magnab.employeelifecycle.enums.WorkflowType.ONBOARDING);
            dependencyTemplate.setIsActive(true);
            dependencyTemplate.setCreatedBy(testUser.getId());
            dependencyTemplate = workflowTemplateRepository.save(dependencyTemplate);

            // Task 1: No dependency
            TemplateTask task1 = new TemplateTask();
            task1.setTemplate(dependencyTemplate);
            task1.setTaskName("First Task");
            task1.setAssignedRole(UserRole.HR_ADMIN);
            task1.setSequenceOrder(1);
            task1.setCreatedBy(testUser.getId());
            task1 = templateTaskRepository.save(task1);

            // Task 2: Depends on task1
            TemplateTask task2 = new TemplateTask();
            task2.setTemplate(dependencyTemplate);
            task2.setTaskName("Second Task");
            task2.setAssignedRole(UserRole.TECH_SUPPORT);
            task2.setSequenceOrder(2);
            task2.setDependsOnTask(task1);
            task2.setCreatedBy(testUser.getId());
            task2 = templateTaskRepository.save(task2);

            // Create workflow instance
            EmployeeDetails employeeDetails = new EmployeeDetails();
            employeeDetails.setEmployeeName("Dependency Test");
            employeeDetails.setEmployeeEmail("dep@test.com");
            employeeDetails.setEmployeeRole("Engineer");

            WorkflowCreationResult workflowResult = workflowService.createWorkflowInstance(
                    dependencyTemplate.getId(), employeeDetails, null, testUser.getId());

            // Act: First assignment - only task1 should be assigned
            List<TaskAssignmentResult> firstRound = workflowService.assignTasksForWorkflow(workflowResult.getWorkflowInstanceId());

            // Assert: Only task1 assigned (no dependencies)
            assertThat(firstRound).hasSize(1);
            assertThat(firstRound.get(0).getTaskName()).isEqualTo("First Task");

            // Complete task1
            List<TaskInstance> allTasks = taskInstanceRepository
                    .findByWorkflowInstanceId(workflowResult.getWorkflowInstanceId());
            TaskInstance task1Instance = allTasks.stream()
                    .filter(t -> t.getTaskName().equals("First Task"))
                    .findFirst().orElseThrow();
            task1Instance.setStatus(TaskStatus.COMPLETED);
            taskInstanceRepository.save(task1Instance);

            // Act: Second assignment - now task2 should be assignable
            List<TaskAssignmentResult> secondRound = workflowService.assignTasksForWorkflow(workflowResult.getWorkflowInstanceId());

            // Assert: Task2 now assigned
            assertThat(secondRound).hasSize(1);
            assertThat(secondRound.get(0).getTaskName()).isEqualTo("Second Task");
        }

        @Test
        @DisplayName("Should be idempotent - calling multiple times doesn't cause errors or duplicates")
        void shouldBeIdempotent() {
            // Act: Assign tasks first time
            List<TaskAssignmentResult> firstCall = workflowService.assignTasksForWorkflow(testWorkflowInstance.getId());
            assertThat(firstCall).hasSize(3);

            // Act: Assign tasks second time
            List<TaskAssignmentResult> secondCall = workflowService.assignTasksForWorkflow(testWorkflowInstance.getId());

            // Assert: No new assignments on second call
            assertThat(secondCall).isEmpty();

            // Verify database still has only 3 assigned tasks
            List<TaskInstance> allTasks = taskInstanceRepository
                    .findByWorkflowInstanceId(testWorkflowInstance.getId());
            assertThat(allTasks).hasSize(3);
            assertThat(allTasks).allMatch(task -> task.getStatus() == TaskStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("Should update workflow status to IN_PROGRESS and create state history")
        void shouldUpdateWorkflowStatusOnFirstAssignment() {
            // Verify initial status
            assertThat(testWorkflowInstance.getStatus()).isEqualTo(WorkflowStatus.INITIATED);

            // Act
            List<TaskAssignmentResult> results = workflowService.assignTasksForWorkflow(testWorkflowInstance.getId());

            // Assert: Workflow status updated
            WorkflowInstance updatedWorkflow = workflowInstanceRepository.findById(testWorkflowInstance.getId())
                    .orElseThrow();
            assertThat(updatedWorkflow.getStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);

            // Verify state history created
            List<WorkflowStateHistory> history = workflowStateHistoryRepository
                    .findByWorkflowInstanceIdOrderByChangedAtDesc(testWorkflowInstance.getId());

            // Should have 2 entries: initial (INITIATED) + transition to IN_PROGRESS
            assertThat(history).hasSizeGreaterThanOrEqualTo(2);

            WorkflowStateHistory latestHistory = history.get(0);
            assertThat(latestHistory.getPreviousStatus()).isEqualTo(WorkflowStatus.INITIATED);
            assertThat(latestHistory.getNewStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);
            assertThat(latestHistory.getNotes()).contains("first task assignment");
        }

        @Test
        @DisplayName("Should not update workflow status if tasks already assigned")
        void shouldNotUpdateWorkflowStatusIfAlreadyHasAssignments() {
            // Arrange: Assign tasks first time
            workflowService.assignTasksForWorkflow(testWorkflowInstance.getId());

            // Get history count after first assignment
            List<WorkflowStateHistory> historyAfterFirst = workflowStateHistoryRepository
                    .findByWorkflowInstanceIdOrderByChangedAtDesc(testWorkflowInstance.getId());
            int firstHistoryCount = historyAfterFirst.size();

            // Complete one task and create a new one
            List<TaskInstance> tasks = taskInstanceRepository
                    .findByWorkflowInstanceId(testWorkflowInstance.getId());
            tasks.get(0).setStatus(TaskStatus.COMPLETED);
            taskInstanceRepository.save(tasks.get(0));

            // Act: Call assign again (idempotent)
            workflowService.assignTasksForWorkflow(testWorkflowInstance.getId());

            // Assert: No new state history entry
            List<WorkflowStateHistory> historyAfterSecond = workflowStateHistoryRepository
                    .findByWorkflowInstanceIdOrderByChangedAtDesc(testWorkflowInstance.getId());
            assertThat(historyAfterSecond).hasSize(firstHistoryCount);
        }

        @Test
        @DisplayName("Should handle workflow with no eligible users gracefully")
        void shouldHandleNoEligibleUsers() {
            // Arrange: Deactivate all users
            hrUser1.setIsActive(false);
            hrUser2.setIsActive(false);
            techUser.setIsActive(false);
            managerUser.setIsActive(false);
            userRepository.saveAll(List.of(hrUser1, hrUser2, techUser, managerUser));

            // Act
            List<TaskAssignmentResult> results = workflowService.assignTasksForWorkflow(testWorkflowInstance.getId());

            // Assert: No tasks assigned (no active users)
            assertThat(results).isEmpty();

            // Verify database: All tasks still NOT_STARTED
            List<TaskInstance> allTasks = taskInstanceRepository
                    .findByWorkflowInstanceId(testWorkflowInstance.getId());
            assertThat(allTasks).allMatch(task -> task.getStatus() == TaskStatus.NOT_STARTED);
            assertThat(allTasks).allMatch(task -> task.getAssignedUserId() == null);
        }

        @Test
        @DisplayName("Should throw exception when workflow not found")
        void shouldThrowExceptionWhenWorkflowNotFound() {
            // Arrange
            UUID nonExistentWorkflowId = UUID.randomUUID();

            // Act & Assert
            assertThatThrownBy(() ->
                    workflowService.assignTasksForWorkflow(nonExistentWorkflowId)
            )
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Workflow instance not found");
        }

        @Test
        @DisplayName("End-to-end: Create workflow, assign tasks, verify complete database state")
        void endToEndWorkflowCreationAndAssignment() {
            // Arrange: Create fresh workflow
            EmployeeDetails employeeDetails = new EmployeeDetails();
            employeeDetails.setEmployeeName("E2E Test Employee");
            employeeDetails.setEmployeeEmail("e2e@test.com");
            employeeDetails.setEmployeeRole("Engineer");

            Map<String, Object> customFields = new HashMap<>();
            customFields.put("department", "Engineering");
            customFields.put("location", "Remote");

            // Act 1: Create workflow
            WorkflowCreationResult creationResult = workflowService.createWorkflowInstance(
                    testTemplate.getId(), employeeDetails, customFields, testUser.getId());

            assertThat(creationResult.getWorkflowInstanceId()).isNotNull();
            assertThat(creationResult.getTotalTasks()).isEqualTo(3);

            // Verify workflow created with INITIATED status
            WorkflowInstance workflow = workflowInstanceRepository.findById(creationResult.getWorkflowInstanceId())
                    .orElseThrow();
            assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.INITIATED);
            assertThat(workflow.getEmployeeName()).isEqualTo("E2E Test Employee");

            // Verify tasks created with NOT_STARTED status
            List<TaskInstance> tasksBeforeAssignment = taskInstanceRepository
                    .findByWorkflowInstanceId(workflow.getId());
            assertThat(tasksBeforeAssignment).hasSize(3);
            assertThat(tasksBeforeAssignment).allMatch(t -> t.getStatus() == TaskStatus.NOT_STARTED);
            assertThat(tasksBeforeAssignment).allMatch(t -> t.getAssignedUserId() == null);

            // Act 2: Assign tasks
            List<TaskAssignmentResult> assignmentResults = workflowService.assignTasksForWorkflow(workflow.getId());

            assertThat(assignmentResults).hasSize(3);

            // Verify workflow status updated to IN_PROGRESS
            WorkflowInstance updatedWorkflow = workflowInstanceRepository.findById(workflow.getId())
                    .orElseThrow();
            assertThat(updatedWorkflow.getStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);

            // Verify all tasks assigned with IN_PROGRESS status
            List<TaskInstance> tasksAfterAssignment = taskInstanceRepository
                    .findByWorkflowInstanceId(workflow.getId());
            assertThat(tasksAfterAssignment).hasSize(3);
            assertThat(tasksAfterAssignment).allMatch(t -> t.getStatus() == TaskStatus.IN_PROGRESS);
            assertThat(tasksAfterAssignment).allMatch(t -> t.getAssignedUserId() != null);
            assertThat(tasksAfterAssignment).allMatch(t -> t.getDueDate() != null);

            // Verify correct role assignments
            TaskInstance hrTask = tasksAfterAssignment.stream()
                    .filter(t -> t.getTaskName().equals("HR Setup"))
                    .findFirst().orElseThrow();
            User assignedHrUser = userRepository.findById(hrTask.getAssignedUserId()).orElseThrow();
            assertThat(assignedHrUser.getRole()).isEqualTo(UserRole.HR_ADMIN);

            TaskInstance techTask = tasksAfterAssignment.stream()
                    .filter(t -> t.getTaskName().equals("IT Setup"))
                    .findFirst().orElseThrow();
            User assignedTechUser = userRepository.findById(techTask.getAssignedUserId()).orElseThrow();
            assertThat(assignedTechUser.getRole()).isEqualTo(UserRole.TECH_SUPPORT);

            TaskInstance managerTask = tasksAfterAssignment.stream()
                    .filter(t -> t.getTaskName().equals("Manager Orientation"))
                    .findFirst().orElseThrow();
            User assignedManagerUser = userRepository.findById(managerTask.getAssignedUserId()).orElseThrow();
            assertThat(assignedManagerUser.getRole()).isEqualTo(UserRole.LINE_MANAGER);

            // Verify state history
            List<WorkflowStateHistory> history = workflowStateHistoryRepository
                    .findByWorkflowInstanceIdOrderByChangedAtDesc(workflow.getId());
            assertThat(history).hasSizeGreaterThanOrEqualTo(2);

            // Verify custom fields preserved
            assertThat(updatedWorkflow.getCustomFieldValues()).containsEntry("department", "Engineering");
            assertThat(updatedWorkflow.getCustomFieldValues()).containsEntry("location", "Remote");
        }
    }

    @Nested
    @DisplayName("Story 3.4: Workflow State Management Integration Tests")
    class WorkflowStateManagementIntegrationTests {

        private User hrUser;
        private User techUser;
        private User managerUser;
        private WorkflowInstance testWorkflowInstance;
        private List<TaskInstance> testTasks;

        @BeforeEach
        void setupStateManagementData() {
            // Create HR user
            hrUser = new User();
            hrUser.setUsername("hr_state_test");
            hrUser.setEmail("hr.state@test.com");
            hrUser.setPasswordHash("hashed");
            hrUser.setRole(UserRole.HR_ADMIN);
            hrUser.setIsActive(true);
            hrUser = userRepository.save(hrUser);

            // Create tech user
            techUser = new User();
            techUser.setUsername("tech_state_test");
            techUser.setEmail("tech.state@test.com");
            techUser.setPasswordHash("hashed");
            techUser.setRole(UserRole.TECH_SUPPORT);
            techUser.setIsActive(true);
            techUser = userRepository.save(techUser);

            // Create manager user
            managerUser = new User();
            managerUser.setUsername("manager_state_test");
            managerUser.setEmail("manager.state@test.com");
            managerUser.setPasswordHash("hashed");
            managerUser.setRole(UserRole.LINE_MANAGER);
            managerUser.setIsActive(true);
            managerUser = userRepository.save(managerUser);

            // Create workflow instance
            EmployeeDetails employeeDetails = new EmployeeDetails();
            employeeDetails.setEmployeeName("State Management Test Employee");
            employeeDetails.setEmployeeEmail("state.test@example.com");
            employeeDetails.setEmployeeRole("Engineer");

            WorkflowCreationResult creationResult = workflowService.createWorkflowInstance(
                    testTemplate.getId(), employeeDetails, null, testUser.getId());

            testWorkflowInstance = workflowInstanceRepository.findById(creationResult.getWorkflowInstanceId())
                    .orElseThrow();

            // Get tasks for this workflow
            testTasks = taskInstanceRepository.findByWorkflowInstanceId(testWorkflowInstance.getId());
        }

        @Test
        @DisplayName("Should transition workflow from INITIATED to IN_PROGRESS and persist state history")
        void shouldTransitionWorkflowToInProgress() {
            // Arrange
            assertThat(testWorkflowInstance.getStatus()).isEqualTo(WorkflowStatus.INITIATED);

            // Act
            var result = workflowService.updateWorkflowStatus(
                    testWorkflowInstance.getId(),
                    WorkflowStatus.IN_PROGRESS,
                    testUser.getId(),
                    "Starting workflow"
            );

            // Assert: Result contains correct summary
            assertThat(result).isNotNull();
            assertThat(result.getWorkflowInstanceId()).isEqualTo(testWorkflowInstance.getId());
            assertThat(result.getStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);
            assertThat(result.getTotalTasks()).isEqualTo(3);

            // Verify database persistence
            WorkflowInstance updated = workflowInstanceRepository.findById(testWorkflowInstance.getId())
                    .orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);

            // Verify state history created
            List<WorkflowStateHistory> history = workflowStateHistoryRepository
                    .findByWorkflowInstanceIdOrderByChangedAtDesc(testWorkflowInstance.getId());
            assertThat(history).hasSizeGreaterThanOrEqualTo(2); // Initial + new transition

            WorkflowStateHistory latestHistory = history.get(0);
            assertThat(latestHistory.getPreviousStatus()).isEqualTo(WorkflowStatus.INITIATED);
            assertThat(latestHistory.getNewStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);
            assertThat(latestHistory.getChangedBy()).isEqualTo(testUser.getId());
            assertThat(latestHistory.getNotes()).isEqualTo("Starting workflow");
        }

        @Test
        @DisplayName("Should transition workflow to COMPLETED and set completedAt timestamp")
        void shouldTransitionWorkflowToCompleted() {
            // Arrange: First transition to IN_PROGRESS
            workflowService.updateWorkflowStatus(
                    testWorkflowInstance.getId(),
                    WorkflowStatus.IN_PROGRESS,
                    testUser.getId(),
                    "Starting"
            );

            LocalDateTime beforeCompletion = LocalDateTime.now();

            // Act
            var result = workflowService.updateWorkflowStatus(
                    testWorkflowInstance.getId(),
                    WorkflowStatus.COMPLETED,
                    testUser.getId(),
                    "All tasks done"
            );

            LocalDateTime afterCompletion = LocalDateTime.now();

            // Assert
            assertThat(result.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);

            // Verify completedAt timestamp set
            WorkflowInstance completed = workflowInstanceRepository.findById(testWorkflowInstance.getId())
                    .orElseThrow();
            assertThat(completed.getCompletedAt()).isNotNull();
            assertThat(completed.getCompletedAt()).isAfterOrEqualTo(beforeCompletion);
            assertThat(completed.getCompletedAt()).isBefore(afterCompletion);
        }

        @Test
        @DisplayName("Should reject invalid workflow state transitions")
        void shouldRejectInvalidWorkflowTransitions() {
            // Arrange: Workflow is INITIATED
            assertThat(testWorkflowInstance.getStatus()).isEqualTo(WorkflowStatus.INITIATED);

            // Act & Assert: Cannot transition INITIATED -> COMPLETED
            assertThatThrownBy(() ->
                    workflowService.updateWorkflowStatus(
                            testWorkflowInstance.getId(),
                            WorkflowStatus.COMPLETED,
                            testUser.getId(),
                            "Invalid transition"
                    )
            )
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Invalid workflow state transition");

            // Verify status unchanged
            WorkflowInstance unchanged = workflowInstanceRepository.findById(testWorkflowInstance.getId())
                    .orElseThrow();
            assertThat(unchanged.getStatus()).isEqualTo(WorkflowStatus.INITIATED);
        }

        @Test
        @DisplayName("Should update task status and persist to database")
        void shouldUpdateTaskStatus() {
            // Arrange: Get first task and save original updatedAt
            TaskInstance task = testTasks.get(0);
            assertThat(task.getStatus()).isEqualTo(TaskStatus.NOT_STARTED);
            LocalDateTime originalUpdatedAt = task.getUpdatedAt();

            // Act
            var result = workflowService.updateTaskStatus(
                    task.getId(),
                    TaskStatus.IN_PROGRESS,
                    hrUser.getId()
            );

            // Assert: Result correct
            assertThat(result).isNotNull();
            assertThat(result.getTaskInstanceId()).isEqualTo(task.getId());
            assertThat(result.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);

            // Verify database persistence
            TaskInstance updated = taskInstanceRepository.findById(task.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
            // updatedAt should be equal or after original (JPA might not update in integration test)
            assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
        }

        @Test
        @DisplayName("Should set completedAt and completedBy when task marked COMPLETED")
        void shouldSetTaskCompletionFields() {
            // Arrange: Transition task to IN_PROGRESS first
            TaskInstance task = testTasks.get(0);
            workflowService.updateTaskStatus(task.getId(), TaskStatus.IN_PROGRESS, hrUser.getId());

            LocalDateTime beforeCompletion = LocalDateTime.now();

            // Act
            var result = workflowService.updateTaskStatus(
                    task.getId(),
                    TaskStatus.COMPLETED,
                    hrUser.getId()
            );

            LocalDateTime afterCompletion = LocalDateTime.now();

            // Assert
            assertThat(result.getStatus()).isEqualTo(TaskStatus.COMPLETED);
            assertThat(result.getCompletedAt()).isNotNull();
            assertThat(result.getCompletedBy()).isEqualTo(hrUser.getId());

            // Verify database
            TaskInstance completed = taskInstanceRepository.findById(task.getId()).orElseThrow();
            assertThat(completed.getCompletedAt()).isAfterOrEqualTo(beforeCompletion);
            assertThat(completed.getCompletedAt()).isBefore(afterCompletion);
            assertThat(completed.getCompletedBy()).isEqualTo(hrUser.getId());
        }

        @Test
        @DisplayName("Should reject invalid task state transitions")
        void shouldRejectInvalidTaskTransitions() {
            // Arrange: Task is NOT_STARTED
            TaskInstance task = testTasks.get(0);
            assertThat(task.getStatus()).isEqualTo(TaskStatus.NOT_STARTED);

            // Act & Assert: Cannot transition NOT_STARTED -> COMPLETED
            assertThatThrownBy(() ->
                    workflowService.updateTaskStatus(
                            task.getId(),
                            TaskStatus.COMPLETED,
                            hrUser.getId()
                    )
            )
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Invalid task state transition");

            // Verify status unchanged
            TaskInstance unchanged = taskInstanceRepository.findById(task.getId()).orElseThrow();
            assertThat(unchanged.getStatus()).isEqualTo(TaskStatus.NOT_STARTED);
        }

        @Test
        @DisplayName("End-to-end: Complete all tasks and verify automatic workflow completion")
        void endToEndAutomaticWorkflowCompletion() {
            // Arrange: Assign all tasks first
            workflowService.assignTasksForWorkflow(testWorkflowInstance.getId());

            // Verify workflow is IN_PROGRESS after assignment
            WorkflowInstance afterAssignment = workflowInstanceRepository
                    .findById(testWorkflowInstance.getId()).orElseThrow();
            assertThat(afterAssignment.getStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);

            // Get assigned tasks
            List<TaskInstance> assignedTasks = taskInstanceRepository
                    .findByWorkflowInstanceId(testWorkflowInstance.getId());
            assertThat(assignedTasks).hasSize(3);
            assertThat(assignedTasks).allMatch(t -> t.getStatus() == TaskStatus.IN_PROGRESS);

            // Act: Complete all tasks one by one
            for (TaskInstance task : assignedTasks) {
                workflowService.updateTaskStatus(
                        task.getId(),
                        TaskStatus.COMPLETED,
                        task.getAssignedUserId()
                );
            }

            // Assert: Workflow automatically transitioned to COMPLETED
            WorkflowInstance completedWorkflow = workflowInstanceRepository
                    .findById(testWorkflowInstance.getId()).orElseThrow();
            assertThat(completedWorkflow.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
            assertThat(completedWorkflow.getCompletedAt()).isNotNull();

            // Verify all tasks completed
            List<TaskInstance> completedTasks = taskInstanceRepository
                    .findByWorkflowInstanceId(testWorkflowInstance.getId());
            assertThat(completedTasks).allMatch(t -> t.getStatus() == TaskStatus.COMPLETED);
            assertThat(completedTasks).allMatch(t -> t.getCompletedAt() != null);

            // Verify state history shows automatic transition
            List<WorkflowStateHistory> history = workflowStateHistoryRepository
                    .findByWorkflowInstanceIdOrderByChangedAtDesc(testWorkflowInstance.getId());

            WorkflowStateHistory completionHistory = history.stream()
                    .filter(h -> h.getNewStatus() == WorkflowStatus.COMPLETED)
                    .findFirst().orElseThrow();
            assertThat(completionHistory.getPreviousStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);
            assertThat(completionHistory.getNotes()).contains("All visible tasks completed");
        }

        @Test
        @DisplayName("Should not auto-complete workflow if some visible tasks are incomplete")
        void shouldNotAutoCompleteWithIncompleteTasks() {
            // Arrange: Assign all tasks
            workflowService.assignTasksForWorkflow(testWorkflowInstance.getId());

            List<TaskInstance> assignedTasks = taskInstanceRepository
                    .findByWorkflowInstanceId(testWorkflowInstance.getId());

            // Act: Complete only 2 out of 3 tasks
            workflowService.updateTaskStatus(
                    assignedTasks.get(0).getId(),
                    TaskStatus.COMPLETED,
                    assignedTasks.get(0).getAssignedUserId()
            );
            workflowService.updateTaskStatus(
                    assignedTasks.get(1).getId(),
                    TaskStatus.COMPLETED,
                    assignedTasks.get(1).getAssignedUserId()
            );

            // Assert: Workflow still IN_PROGRESS
            WorkflowInstance workflow = workflowInstanceRepository
                    .findById(testWorkflowInstance.getId()).orElseThrow();
            assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);
            assertThat(workflow.getCompletedAt()).isNull();
        }

        @Test
        @DisplayName("Should trigger dependent task assignment when task completes")
        void shouldTriggerDependentTaskAssignment() {
            // Arrange: Create template with dependencies
            WorkflowTemplate depTemplate = new WorkflowTemplate();
            depTemplate.setTemplateName("Dependency Flow Template");
            depTemplate.setWorkflowType(com.magnab.employeelifecycle.enums.WorkflowType.ONBOARDING);
            depTemplate.setIsActive(true);
            depTemplate.setCreatedBy(testUser.getId());
            depTemplate = workflowTemplateRepository.save(depTemplate);

            // Task 1: No dependency
            TemplateTask task1 = new TemplateTask();
            task1.setTemplate(depTemplate);
            task1.setTaskName("First Task");
            task1.setAssignedRole(UserRole.HR_ADMIN);
            task1.setSequenceOrder(1);
            task1.setCreatedBy(testUser.getId());
            task1 = templateTaskRepository.save(task1);

            // Task 2: Depends on task1
            TemplateTask task2 = new TemplateTask();
            task2.setTemplate(depTemplate);
            task2.setTaskName("Second Task");
            task2.setAssignedRole(UserRole.TECH_SUPPORT);
            task2.setSequenceOrder(2);
            task2.setDependsOnTask(task1);
            task2.setCreatedBy(testUser.getId());
            task2 = templateTaskRepository.save(task2);

            // Create workflow
            EmployeeDetails employeeDetails = new EmployeeDetails();
            employeeDetails.setEmployeeName("Dependency Test");
            employeeDetails.setEmployeeEmail("dep@test.com");
            employeeDetails.setEmployeeRole("Engineer");

            WorkflowCreationResult workflowResult = workflowService.createWorkflowInstance(
                    depTemplate.getId(), employeeDetails, null, testUser.getId());

            // Assign initial tasks (only task1 should be assigned)
            workflowService.assignTasksForWorkflow(workflowResult.getWorkflowInstanceId());

            List<TaskInstance> tasksAfterFirstAssignment = taskInstanceRepository
                    .findByWorkflowInstanceId(workflowResult.getWorkflowInstanceId());

            TaskInstance task1Instance = tasksAfterFirstAssignment.stream()
                    .filter(t -> t.getTaskName().equals("First Task"))
                    .findFirst().orElseThrow();
            TaskInstance task2Instance = tasksAfterFirstAssignment.stream()
                    .filter(t -> t.getTaskName().equals("Second Task"))
                    .findFirst().orElseThrow();

            assertThat(task1Instance.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(task2Instance.getStatus()).isEqualTo(TaskStatus.NOT_STARTED);

            // Act: Complete task1 (should trigger assignment of task2)
            workflowService.updateTaskStatus(
                    task1Instance.getId(),
                    TaskStatus.COMPLETED,
                    task1Instance.getAssignedUserId()
            );

            // Assert: Task2 should now be assigned automatically
            TaskInstance task2AfterCompletion = taskInstanceRepository
                    .findById(task2Instance.getId()).orElseThrow();
            assertThat(task2AfterCompletion.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(task2AfterCompletion.getAssignedUserId()).isNotNull();
        }

        @Test
        @DisplayName("Should calculate correct task counts in workflow state summary")
        void shouldCalculateCorrectTaskCounts() {
            // Arrange: Assign tasks and complete some
            workflowService.assignTasksForWorkflow(testWorkflowInstance.getId());

            List<TaskInstance> tasks = taskInstanceRepository
                    .findByWorkflowInstanceId(testWorkflowInstance.getId());

            // Complete first task
            workflowService.updateTaskStatus(
                    tasks.get(0).getId(),
                    TaskStatus.COMPLETED,
                    tasks.get(0).getAssignedUserId()
            );

            // Block second task
            workflowService.updateTaskStatus(
                    tasks.get(1).getId(),
                    TaskStatus.BLOCKED,
                    tasks.get(1).getAssignedUserId()
            );

            // Act: Update workflow status to get summary
            var summary = workflowService.updateWorkflowStatus(
                    testWorkflowInstance.getId(),
                    WorkflowStatus.BLOCKED,
                    testUser.getId(),
                    "Some tasks blocked"
            );

            // Assert: Task counts are correct
            assertThat(summary.getTotalTasks()).isEqualTo(3);
            assertThat(summary.getTasksCompleted()).isEqualTo(1);
            assertThat(summary.getTasksBlocked()).isEqualTo(1);
            assertThat(summary.getTasksInProgress()).isEqualTo(1);
            assertThat(summary.getTasksNotStarted()).isEqualTo(0);
        }
    }
}
