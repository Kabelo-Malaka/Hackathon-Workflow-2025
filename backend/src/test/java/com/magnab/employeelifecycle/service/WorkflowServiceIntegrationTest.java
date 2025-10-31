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
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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
}
