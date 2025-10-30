package com.magnab.employeelifecycle.repository;

import com.magnab.employeelifecycle.entity.TemplateTask;
import com.magnab.employeelifecycle.entity.WorkflowTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TemplateTaskRepository using TestContainers.
 * Tests repository query methods against a real PostgreSQL database.
 */
@SpringBootTest
@Testcontainers
class TemplateTaskRepositoryTest {

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
    private TemplateTaskRepository templateTaskRepository;

    @Autowired
    private WorkflowTemplateRepository workflowTemplateRepository;

    @Test
    void findByTemplateId_WithSeedData_ReturnsTasks() {
        // Arrange - seed data template ID from changeset 008
        UUID seedTemplateId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        // Act
        List<TemplateTask> results = templateTaskRepository.findByTemplateId(seedTemplateId);

        // Assert
        assertFalse(results.isEmpty(), "Should find tasks for seed template");
        assertTrue(results.size() >= 5, "Seed data should have at least 5 tasks");
        assertTrue(results.stream().allMatch(task -> task.getTemplate().getId().equals(seedTemplateId)),
                "All tasks should belong to the template");
    }

    @Test
    void findByTemplateId_WithNonExistentId_ReturnsEmptyList() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act
        List<TemplateTask> results = templateTaskRepository.findByTemplateId(nonExistentId);

        // Assert
        assertTrue(results.isEmpty(), "Should not find tasks for non-existent template");
    }

    @Test
    void findByTemplateIdOrderBySequenceOrder_ReturnsSortedTasks() {
        // Arrange - seed data template ID
        UUID seedTemplateId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        // Act
        List<TemplateTask> results = templateTaskRepository.findByTemplateIdOrderBySequenceOrder(seedTemplateId);

        // Assert
        assertFalse(results.isEmpty(), "Should find tasks");
        assertTrue(results.size() >= 5, "Should have at least 5 tasks from seed data");

        // Verify tasks are sorted by sequence_order
        for (int i = 0; i < results.size() - 1; i++) {
            assertTrue(results.get(i).getSequenceOrder() <= results.get(i + 1).getSequenceOrder(),
                    "Tasks should be ordered by sequence_order");
        }

        // Verify first task from seed data
        assertEquals(1, results.get(0).getSequenceOrder(), "First task should have sequence 1");
        assertEquals("Create user account", results.get(0).getTaskName());

        // Find the "Grant system access" task from seed data (sequence 5)
        assertTrue(results.stream().anyMatch(t -> t.getSequenceOrder() == 5 && t.getTaskName().equals("Grant system access")),
                "Should include the 'Grant system access' task with sequence 5 from seed data");
    }

    @Test
    void save_WithNewTask_PersistsSuccessfully() {
        // Arrange - get existing template
        WorkflowTemplate template = workflowTemplateRepository
            .findByTemplateName("Standard Employee Onboarding")
            .orElseThrow(() -> new AssertionError("Seed template should exist"));

        TemplateTask newTask = new TemplateTask();
        newTask.setTemplate(template);
        newTask.setTaskName("Test Task " + System.currentTimeMillis());
        newTask.setDescription("Test task description");
        newTask.setSequenceOrder(10);

        // Act
        TemplateTask saved = templateTaskRepository.save(newTask);

        // Assert
        assertNotNull(saved.getId(), "Saved task should have generated ID");
        assertEquals(newTask.getTaskName(), saved.getTaskName());
        assertEquals(10, saved.getSequenceOrder());
        assertNotNull(saved.getCreatedAt(), "Created timestamp should be set");
        assertEquals(template.getId(), saved.getTemplate().getId());
    }

    @Test
    void save_WithDependencyTask_PersistsSuccessfully() {
        // Arrange - get existing template and task
        UUID seedTemplateId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        List<TemplateTask> existingTasks = templateTaskRepository.findByTemplateId(seedTemplateId);
        assertFalse(existingTasks.isEmpty(), "Should have existing tasks");

        TemplateTask dependsOnTask = existingTasks.get(0); // First task
        WorkflowTemplate template = dependsOnTask.getTemplate();

        TemplateTask newTask = new TemplateTask();
        newTask.setTemplate(template);
        newTask.setTaskName("Dependent Task " + System.currentTimeMillis());
        newTask.setDescription("Task that depends on another task");
        newTask.setSequenceOrder(20);
        newTask.setDependsOnTask(dependsOnTask);

        // Act
        TemplateTask saved = templateTaskRepository.save(newTask);

        // Assert
        assertNotNull(saved.getId(), "Saved task should have generated ID");
        assertNotNull(saved.getDependsOnTask(), "Dependency should be set");
        assertEquals(dependsOnTask.getId(), saved.getDependsOnTask().getId(),
                "Dependency should reference the correct task");
    }

    @Test
    void findAll_ReturnsAllTasks() {
        // Act
        List<TemplateTask> allTasks = templateTaskRepository.findAll();

        // Assert
        assertFalse(allTasks.isEmpty(), "Should find tasks (at least seed data)");
        assertTrue(allTasks.size() >= 5, "Should have at least 5 tasks from seed data");
    }

    @Test
    void save_WithNullTemplate_ThrowsException() {
        // Arrange
        TemplateTask taskWithoutTemplate = new TemplateTask();
        taskWithoutTemplate.setTaskName("Task without template");
        taskWithoutTemplate.setDescription("This should fail");
        taskWithoutTemplate.setSequenceOrder(1);
        // template is null

        // Act & Assert
        assertThrows(Exception.class, () -> {
            templateTaskRepository.save(taskWithoutTemplate);
            templateTaskRepository.flush(); // Force immediate constraint check
        }, "Saving task without template should throw exception");
    }

    @Test
    void save_WithNonExistentTemplateId_ThrowsException() {
        // Arrange
        WorkflowTemplate nonPersistedTemplate = new WorkflowTemplate();
        nonPersistedTemplate.setId(UUID.randomUUID()); // ID not in database

        TemplateTask taskWithInvalidTemplate = new TemplateTask();
        taskWithInvalidTemplate.setTemplate(nonPersistedTemplate);
        taskWithInvalidTemplate.setTaskName("Task with invalid template");
        taskWithInvalidTemplate.setDescription("This should fail FK constraint");
        taskWithInvalidTemplate.setSequenceOrder(1);

        // Act & Assert
        assertThrows(Exception.class, () -> {
            templateTaskRepository.save(taskWithInvalidTemplate);
            templateTaskRepository.flush(); // Force immediate constraint check
        }, "Saving task with non-existent template should throw FK constraint violation");
    }

    @Test
    void save_WithNullSequenceOrder_ThrowsException() {
        // Arrange
        WorkflowTemplate template = workflowTemplateRepository
            .findByTemplateName("Standard Employee Onboarding")
            .orElseThrow(() -> new AssertionError("Seed template should exist"));

        TemplateTask taskWithoutSequence = new TemplateTask();
        taskWithoutSequence.setTemplate(template);
        taskWithoutSequence.setTaskName("Task without sequence");
        taskWithoutSequence.setDescription("This should fail");
        // sequenceOrder is null

        // Act & Assert
        assertThrows(Exception.class, () -> {
            templateTaskRepository.save(taskWithoutSequence);
            templateTaskRepository.flush(); // Force immediate constraint check
        }, "Saving task without sequence order should throw exception");
    }

    @Test
    void delete_CascadeFromTemplate_DeletesTasks() {
        // Arrange - create a template and tasks
        WorkflowTemplate template = new WorkflowTemplate();
        template.setTemplateName("Temp Template " + System.currentTimeMillis());
        template.setWorkflowType(com.magnab.employeelifecycle.enums.WorkflowType.ONBOARDING);
        template.setDescription("Template to test cascade delete");
        template.setDefaultStatus(com.magnab.employeelifecycle.enums.WorkflowStatus.INITIATED);
        WorkflowTemplate savedTemplate = workflowTemplateRepository.save(template);

        TemplateTask task1 = new TemplateTask();
        task1.setTemplate(savedTemplate);
        task1.setTaskName("Task 1");
        task1.setDescription("First task");
        task1.setSequenceOrder(1);
        templateTaskRepository.save(task1);

        TemplateTask task2 = new TemplateTask();
        task2.setTemplate(savedTemplate);
        task2.setTaskName("Task 2");
        task2.setDescription("Second task");
        task2.setSequenceOrder(2);
        templateTaskRepository.save(task2);

        UUID templateId = savedTemplate.getId();

        // Verify tasks exist
        List<TemplateTask> tasksBefore = templateTaskRepository.findByTemplateId(templateId);
        assertEquals(2, tasksBefore.size(), "Should have 2 tasks before deletion");

        // Act - delete the template (should cascade to tasks)
        workflowTemplateRepository.delete(savedTemplate);
        workflowTemplateRepository.flush();

        // Assert - tasks should be deleted via cascade
        List<TemplateTask> tasksAfter = templateTaskRepository.findByTemplateId(templateId);
        assertTrue(tasksAfter.isEmpty(), "Tasks should be deleted when template is deleted (CASCADE)");
    }
}
