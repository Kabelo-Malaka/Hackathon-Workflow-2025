package com.magnab.employeelifecycle.repository;

import com.magnab.employeelifecycle.entity.TaskInstance;
import com.magnab.employeelifecycle.entity.WorkflowInstance;
import com.magnab.employeelifecycle.enums.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TaskInstanceRepository using TestContainers.
 * Tests repository query methods, cascade deletes, and JSONB storage.
 */
@SpringBootTest
@Testcontainers
class TaskInstanceRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TaskInstanceRepository taskInstanceRepository;

    @Autowired
    private WorkflowInstanceRepository workflowInstanceRepository;

    @Autowired
    private TemplateTaskRepository templateTaskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkflowTemplateRepository workflowTemplateRepository;

    @Test
    void save_WithAllRequiredFields_PersistsSuccessfully() {
        // Arrange
        UUID adminUserId = userRepository.findAll().get(0).getId();
        UUID templateId = workflowTemplateRepository.findAll().get(0).getId();
        UUID templateTaskId = templateTaskRepository.findAll().get(0).getId();

        // Create a workflow instance first
        WorkflowInstance workflowInstance = createTestWorkflowInstance(adminUserId, templateId);
        WorkflowInstance savedWorkflow = workflowInstanceRepository.save(workflowInstance);

        // Create task instance
        TaskInstance taskInstance = new TaskInstance();
        taskInstance.setWorkflowInstanceId(savedWorkflow.getId());
        taskInstance.setTemplateTaskId(templateTaskId);
        taskInstance.setTaskName("Test Task");
        taskInstance.setAssignedUserId(adminUserId);
        taskInstance.setAssignedRole(UserRole.HR_ADMIN);
        taskInstance.setStatus(TaskStatus.NOT_STARTED);
        taskInstance.setIsVisible(true);

        // Act
        TaskInstance saved = taskInstanceRepository.save(taskInstance);
        taskInstanceRepository.flush();

        // Assert
        assertNotNull(saved.getId(), "Task instance should have generated ID");
        assertEquals("Test Task", saved.getTaskName());
        assertEquals(TaskStatus.NOT_STARTED, saved.getStatus());
        assertEquals(UserRole.HR_ADMIN, saved.getAssignedRole());
        assertTrue(saved.getIsVisible(), "Default visibility should be true");
        assertNotNull(saved.getCreatedAt(), "Created timestamp should be set");
        assertNotNull(saved.getUpdatedAt(), "Updated timestamp should be set");
    }

    @Test
    void save_WithChecklistData_StoresJsonb() {
        // Arrange
        UUID adminUserId = userRepository.findAll().get(0).getId();
        UUID templateId = workflowTemplateRepository.findAll().get(0).getId();
        UUID templateTaskId = templateTaskRepository.findAll().get(0).getId();

        WorkflowInstance workflowInstance = createTestWorkflowInstance(adminUserId, templateId);
        WorkflowInstance savedWorkflow = workflowInstanceRepository.save(workflowInstance);

        Map<String, Object> checklistData = new HashMap<>();
        Map<String, Object> item1 = new HashMap<>();
        item1.put("id", "item1");
        item1.put("description", "Laptop");
        item1.put("checked", true);
        item1.put("identifier", "LP-12345");

        Map<String, Object> item2 = new HashMap<>();
        item2.put("id", "item2");
        item2.put("description", "Monitor");
        item2.put("checked", false);

        checklistData.put("items", List.of(item1, item2));
        checklistData.put("partial_save_at", LocalDateTime.now().toString());

        TaskInstance taskInstance = new TaskInstance();
        taskInstance.setWorkflowInstanceId(savedWorkflow.getId());
        taskInstance.setTemplateTaskId(templateTaskId);
        taskInstance.setTaskName("Hardware Provisioning");
        taskInstance.setAssignedUserId(adminUserId);
        taskInstance.setAssignedRole(UserRole.TECH_SUPPORT);
        taskInstance.setStatus(TaskStatus.IN_PROGRESS);
        taskInstance.setChecklistData(checklistData);

        // Act
        TaskInstance saved = taskInstanceRepository.save(taskInstance);
        taskInstanceRepository.flush();

        // Assert
        assertNotNull(saved.getId());
        assertNotNull(saved.getChecklistData(), "Checklist data should be persisted");
        assertTrue(saved.getChecklistData().containsKey("items"));
        assertTrue(saved.getChecklistData().containsKey("partial_save_at"));
    }

    @Test
    void findByWorkflowInstanceId_ReturnsAllTasksForWorkflow() {
        // Arrange
        UUID adminUserId = userRepository.findAll().get(0).getId();
        UUID templateId = workflowTemplateRepository.findAll().get(0).getId();
        UUID templateTaskId = templateTaskRepository.findAll().get(0).getId();

        WorkflowInstance workflowInstance = createTestWorkflowInstance(adminUserId, templateId);
        WorkflowInstance savedWorkflow = workflowInstanceRepository.save(workflowInstance);

        // Create two tasks for the workflow
        TaskInstance task1 = createTestTaskInstance(savedWorkflow.getId(), templateTaskId, "Task 1", adminUserId);
        TaskInstance task2 = createTestTaskInstance(savedWorkflow.getId(), templateTaskId, "Task 2", adminUserId);
        taskInstanceRepository.save(task1);
        taskInstanceRepository.save(task2);

        // Act
        List<TaskInstance> results = taskInstanceRepository.findByWorkflowInstanceId(savedWorkflow.getId());

        // Assert
        assertTrue(results.size() >= 2, "Should find at least 2 tasks for the workflow");
        assertTrue(results.stream().anyMatch(t -> t.getTaskName().equals("Task 1")));
        assertTrue(results.stream().anyMatch(t -> t.getTaskName().equals("Task 2")));
    }

    @Test
    void findByAssignedUserId_ReturnsUserTasks() {
        // Arrange
        UUID adminUserId = userRepository.findAll().get(0).getId();
        UUID templateId = workflowTemplateRepository.findAll().get(0).getId();
        UUID templateTaskId = templateTaskRepository.findAll().get(0).getId();

        WorkflowInstance workflowInstance = createTestWorkflowInstance(adminUserId, templateId);
        WorkflowInstance savedWorkflow = workflowInstanceRepository.save(workflowInstance);

        TaskInstance taskInstance = createTestTaskInstance(savedWorkflow.getId(), templateTaskId, "User Task", adminUserId);
        taskInstanceRepository.save(taskInstance);

        // Act
        List<TaskInstance> results = taskInstanceRepository.findByAssignedUserId(adminUserId);

        // Assert
        assertFalse(results.isEmpty(), "Should find tasks assigned to user");
        assertTrue(results.stream().anyMatch(t -> t.getAssignedUserId().equals(adminUserId)));
    }

    @Test
    void findByAssignedUserIdAndStatus_ReturnsFilteredTasks() {
        // Arrange
        UUID adminUserId = userRepository.findAll().get(0).getId();
        UUID templateId = workflowTemplateRepository.findAll().get(0).getId();
        UUID templateTaskId = templateTaskRepository.findAll().get(0).getId();

        WorkflowInstance workflowInstance = createTestWorkflowInstance(adminUserId, templateId);
        WorkflowInstance savedWorkflow = workflowInstanceRepository.save(workflowInstance);

        TaskInstance task1 = createTestTaskInstance(savedWorkflow.getId(), templateTaskId, "Not Started Task", adminUserId);
        task1.setStatus(TaskStatus.NOT_STARTED);
        TaskInstance task2 = createTestTaskInstance(savedWorkflow.getId(), templateTaskId, "In Progress Task", adminUserId);
        task2.setStatus(TaskStatus.IN_PROGRESS);
        taskInstanceRepository.save(task1);
        taskInstanceRepository.save(task2);

        // Act
        List<TaskInstance> results = taskInstanceRepository
            .findByAssignedUserIdAndStatus(adminUserId, TaskStatus.IN_PROGRESS);

        // Assert
        assertFalse(results.isEmpty(), "Should find tasks for user with specific status");
        assertTrue(results.stream().allMatch(t -> t.getStatus() == TaskStatus.IN_PROGRESS));
    }

    @Test
    void findByWorkflowInstanceIdAndIsVisible_ReturnsVisibleTasks() {
        // Arrange
        UUID adminUserId = userRepository.findAll().get(0).getId();
        UUID templateId = workflowTemplateRepository.findAll().get(0).getId();
        UUID templateTaskId = templateTaskRepository.findAll().get(0).getId();

        WorkflowInstance workflowInstance = createTestWorkflowInstance(adminUserId, templateId);
        WorkflowInstance savedWorkflow = workflowInstanceRepository.save(workflowInstance);

        TaskInstance visibleTask = createTestTaskInstance(savedWorkflow.getId(), templateTaskId, "Visible Task", adminUserId);
        visibleTask.setIsVisible(true);
        TaskInstance hiddenTask = createTestTaskInstance(savedWorkflow.getId(), templateTaskId, "Hidden Task", adminUserId);
        hiddenTask.setIsVisible(false);
        taskInstanceRepository.save(visibleTask);
        taskInstanceRepository.save(hiddenTask);

        // Act
        List<TaskInstance> results = taskInstanceRepository
            .findByWorkflowInstanceIdAndIsVisible(savedWorkflow.getId(), true);

        // Assert
        assertFalse(results.isEmpty(), "Should find visible tasks");
        assertTrue(results.stream().allMatch(TaskInstance::getIsVisible));
    }

    @Test
    void delete_WhenWorkflowDeleted_CascadesDelete() {
        // Arrange
        UUID adminUserId = userRepository.findAll().get(0).getId();
        UUID templateId = workflowTemplateRepository.findAll().get(0).getId();
        UUID templateTaskId = templateTaskRepository.findAll().get(0).getId();

        WorkflowInstance workflowInstance = createTestWorkflowInstance(adminUserId, templateId);
        WorkflowInstance savedWorkflow = workflowInstanceRepository.save(workflowInstance);

        TaskInstance taskInstance = createTestTaskInstance(savedWorkflow.getId(), templateTaskId, "Cascade Test", adminUserId);
        TaskInstance savedTask = taskInstanceRepository.save(taskInstance);
        UUID taskId = savedTask.getId();

        // Verify task exists
        assertTrue(taskInstanceRepository.findById(taskId).isPresent(),
            "Task should exist before workflow deletion");

        // Act - delete the workflow
        workflowInstanceRepository.delete(savedWorkflow);
        workflowInstanceRepository.flush();

        // Assert - task should be deleted due to CASCADE
        assertFalse(taskInstanceRepository.findById(taskId).isPresent(),
            "Task should be deleted when workflow is deleted (CASCADE)");
    }

    @Test
    void save_WithNullAssignedUserId_AllowsNull() {
        // Arrange
        UUID adminUserId = userRepository.findAll().get(0).getId();
        UUID templateId = workflowTemplateRepository.findAll().get(0).getId();
        UUID templateTaskId = templateTaskRepository.findAll().get(0).getId();

        WorkflowInstance workflowInstance = createTestWorkflowInstance(adminUserId, templateId);
        WorkflowInstance savedWorkflow = workflowInstanceRepository.save(workflowInstance);

        TaskInstance taskInstance = new TaskInstance();
        taskInstance.setWorkflowInstanceId(savedWorkflow.getId());
        taskInstance.setTemplateTaskId(templateTaskId);
        taskInstance.setTaskName("Unassigned Task");
        taskInstance.setAssignedUserId(null); // No user assigned
        taskInstance.setAssignedRole(UserRole.HR_ADMIN);
        taskInstance.setStatus(TaskStatus.NOT_STARTED);

        // Act
        TaskInstance saved = taskInstanceRepository.save(taskInstance);
        taskInstanceRepository.flush();

        // Assert
        assertNotNull(saved.getId());
        assertNull(saved.getAssignedUserId(), "Task can be unassigned");
    }

    @Test
    void save_WithInvalidWorkflowInstanceId_ThrowsException() {
        // Arrange
        UUID templateTaskId = templateTaskRepository.findAll().get(0).getId();
        UUID adminUserId = userRepository.findAll().get(0).getId();
        UUID nonExistentWorkflowId = UUID.randomUUID();

        TaskInstance taskInstance = new TaskInstance();
        taskInstance.setWorkflowInstanceId(nonExistentWorkflowId);
        taskInstance.setTemplateTaskId(templateTaskId);
        taskInstance.setTaskName("Invalid Workflow Task");
        taskInstance.setAssignedUserId(adminUserId);
        taskInstance.setAssignedRole(UserRole.HR_ADMIN);
        taskInstance.setStatus(TaskStatus.NOT_STARTED);

        // Act & Assert
        assertThrows(Exception.class, () -> {
            taskInstanceRepository.save(taskInstance);
            taskInstanceRepository.flush();
        }, "Should throw foreign key constraint violation for invalid workflow_instance_id");
    }

    @Test
    void save_WithDefaultValues_AppliesDefaults() {
        // Arrange
        UUID adminUserId = userRepository.findAll().get(0).getId();
        UUID templateId = workflowTemplateRepository.findAll().get(0).getId();
        UUID templateTaskId = templateTaskRepository.findAll().get(0).getId();

        WorkflowInstance workflowInstance = createTestWorkflowInstance(adminUserId, templateId);
        WorkflowInstance savedWorkflow = workflowInstanceRepository.save(workflowInstance);

        TaskInstance taskInstance = new TaskInstance();
        taskInstance.setWorkflowInstanceId(savedWorkflow.getId());
        taskInstance.setTemplateTaskId(templateTaskId);
        taskInstance.setTaskName("Default Values Task");
        taskInstance.setAssignedRole(UserRole.HR_ADMIN);
        // status and isVisible not explicitly set

        // Act
        TaskInstance saved = taskInstanceRepository.save(taskInstance);
        taskInstanceRepository.flush();

        // Assert
        assertEquals(TaskStatus.NOT_STARTED, saved.getStatus(),
            "Default status should be NOT_STARTED");
        assertTrue(saved.getIsVisible(), "Default visibility should be true");
    }

    // Helper methods
    private WorkflowInstance createTestWorkflowInstance(UUID initiatedBy, UUID templateId) {
        WorkflowInstance workflowInstance = new WorkflowInstance();
        workflowInstance.setTemplateId(templateId);
        workflowInstance.setEmployeeName("Test Employee " + System.currentTimeMillis());
        workflowInstance.setEmployeeEmail("test" + System.currentTimeMillis() + "@example.com");
        workflowInstance.setEmployeeRole("Developer");
        workflowInstance.setWorkflowType(WorkflowType.ONBOARDING);
        workflowInstance.setStatus(WorkflowStatus.INITIATED);
        workflowInstance.setInitiatedBy(initiatedBy);
        workflowInstance.setInitiatedAt(LocalDateTime.now());
        return workflowInstance;
    }

    private TaskInstance createTestTaskInstance(UUID workflowInstanceId, UUID templateTaskId, String taskName, UUID assignedUserId) {
        TaskInstance taskInstance = new TaskInstance();
        taskInstance.setWorkflowInstanceId(workflowInstanceId);
        taskInstance.setTemplateTaskId(templateTaskId);
        taskInstance.setTaskName(taskName);
        taskInstance.setAssignedUserId(assignedUserId);
        taskInstance.setAssignedRole(UserRole.HR_ADMIN);
        taskInstance.setStatus(TaskStatus.NOT_STARTED);
        taskInstance.setIsVisible(true);
        return taskInstance;
    }
}
