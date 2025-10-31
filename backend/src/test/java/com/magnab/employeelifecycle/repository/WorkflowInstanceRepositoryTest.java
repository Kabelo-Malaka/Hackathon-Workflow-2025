package com.magnab.employeelifecycle.repository;

import com.magnab.employeelifecycle.entity.WorkflowInstance;
import com.magnab.employeelifecycle.enums.WorkflowStatus;
import com.magnab.employeelifecycle.enums.WorkflowType;
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
 * Integration tests for WorkflowInstanceRepository using TestContainers.
 * Tests repository query methods against a real PostgreSQL database.
 */
@SpringBootTest
@Testcontainers
class WorkflowInstanceRepositoryTest {

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
    private WorkflowInstanceRepository workflowInstanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkflowTemplateRepository workflowTemplateRepository;

    @Test
    void save_WithAllRequiredFields_PersistsSuccessfully() {
        // Arrange
        UUID adminUserId = userRepository.findAll().get(0).getId(); // Use seed admin user
        UUID templateId = workflowTemplateRepository.findAll().get(0).getId(); // Use seed template

        WorkflowInstance workflowInstance = new WorkflowInstance();
        workflowInstance.setTemplateId(templateId);
        workflowInstance.setEmployeeName("John Doe");
        workflowInstance.setEmployeeEmail("john.doe@example.com");
        workflowInstance.setEmployeeRole("Software Engineer");
        workflowInstance.setWorkflowType(WorkflowType.ONBOARDING);
        workflowInstance.setStatus(WorkflowStatus.INITIATED);
        workflowInstance.setInitiatedBy(adminUserId);
        workflowInstance.setInitiatedAt(LocalDateTime.now());

        // Act
        WorkflowInstance saved = workflowInstanceRepository.save(workflowInstance);
        workflowInstanceRepository.flush();

        // Assert
        assertNotNull(saved.getId(), "Workflow instance should have generated ID");
        assertEquals("John Doe", saved.getEmployeeName());
        assertEquals("john.doe@example.com", saved.getEmployeeEmail());
        assertEquals(WorkflowType.ONBOARDING, saved.getWorkflowType());
        assertEquals(WorkflowStatus.INITIATED, saved.getStatus());
        assertNotNull(saved.getCreatedAt(), "Created timestamp should be set");
        assertNotNull(saved.getUpdatedAt(), "Updated timestamp should be set");
    }

    @Test
    void save_WithCustomFieldValues_StoresJsonb() {
        // Arrange
        UUID adminUserId = userRepository.findAll().get(0).getId();
        UUID templateId = workflowTemplateRepository.findAll().get(0).getId();

        Map<String, Object> customFields = new HashMap<>();
        customFields.put("start_date", "2025-11-15");
        customFields.put("department", "Engineering");
        customFields.put("remote_status", "hybrid");

        WorkflowInstance workflowInstance = new WorkflowInstance();
        workflowInstance.setTemplateId(templateId);
        workflowInstance.setEmployeeName("Jane Smith");
        workflowInstance.setEmployeeEmail("jane.smith@example.com");
        workflowInstance.setEmployeeRole("Product Manager");
        workflowInstance.setWorkflowType(WorkflowType.ONBOARDING);
        workflowInstance.setStatus(WorkflowStatus.INITIATED);
        workflowInstance.setInitiatedBy(adminUserId);
        workflowInstance.setInitiatedAt(LocalDateTime.now());
        workflowInstance.setCustomFieldValues(customFields);

        // Act
        WorkflowInstance saved = workflowInstanceRepository.save(workflowInstance);
        workflowInstanceRepository.flush();

        // Assert
        assertNotNull(saved.getId());
        assertNotNull(saved.getCustomFieldValues(), "Custom field values should be persisted");
        assertEquals("Engineering", saved.getCustomFieldValues().get("department"));
        assertEquals("hybrid", saved.getCustomFieldValues().get("remote_status"));
    }

    @Test
    void findByEmployeeEmail_WithExistingEmail_ReturnsWorkflows() {
        // Arrange
        UUID adminUserId = userRepository.findAll().get(0).getId();
        UUID templateId = workflowTemplateRepository.findAll().get(0).getId();
        String testEmail = "test.employee@example.com";

        WorkflowInstance workflowInstance = new WorkflowInstance();
        workflowInstance.setTemplateId(templateId);
        workflowInstance.setEmployeeName("Test Employee");
        workflowInstance.setEmployeeEmail(testEmail);
        workflowInstance.setEmployeeRole("Developer");
        workflowInstance.setWorkflowType(WorkflowType.ONBOARDING);
        workflowInstance.setStatus(WorkflowStatus.INITIATED);
        workflowInstance.setInitiatedBy(adminUserId);
        workflowInstance.setInitiatedAt(LocalDateTime.now());
        workflowInstanceRepository.save(workflowInstance);

        // Act
        List<WorkflowInstance> results = workflowInstanceRepository.findByEmployeeEmail(testEmail);

        // Assert
        assertFalse(results.isEmpty(), "Should find workflow by employee email");
        assertTrue(results.stream().anyMatch(w -> w.getEmployeeEmail().equals(testEmail)));
    }

    @Test
    void findByStatus_WithInitiated_ReturnsInitiatedWorkflows() {
        // Arrange
        UUID adminUserId = userRepository.findAll().get(0).getId();
        UUID templateId = workflowTemplateRepository.findAll().get(0).getId();

        WorkflowInstance workflowInstance = new WorkflowInstance();
        workflowInstance.setTemplateId(templateId);
        workflowInstance.setEmployeeName("Status Test");
        workflowInstance.setEmployeeEmail("status.test@example.com");
        workflowInstance.setEmployeeRole("Analyst");
        workflowInstance.setWorkflowType(WorkflowType.ONBOARDING);
        workflowInstance.setStatus(WorkflowStatus.INITIATED);
        workflowInstance.setInitiatedBy(adminUserId);
        workflowInstance.setInitiatedAt(LocalDateTime.now());
        workflowInstanceRepository.save(workflowInstance);

        // Act
        List<WorkflowInstance> results = workflowInstanceRepository.findByStatus(WorkflowStatus.INITIATED);

        // Assert
        assertFalse(results.isEmpty(), "Should find workflows with INITIATED status");
        assertTrue(results.stream().allMatch(w -> w.getStatus() == WorkflowStatus.INITIATED));
    }

    @Test
    void findByStatusOrderByInitiatedAtDesc_ReturnsOrderedWorkflows() {
        // Arrange
        UUID adminUserId = userRepository.findAll().get(0).getId();
        UUID templateId = workflowTemplateRepository.findAll().get(0).getId();

        // Create two workflows with different initiation times
        WorkflowInstance workflow1 = new WorkflowInstance();
        workflow1.setTemplateId(templateId);
        workflow1.setEmployeeName("First Employee");
        workflow1.setEmployeeEmail("first@example.com");
        workflow1.setEmployeeRole("Developer");
        workflow1.setWorkflowType(WorkflowType.ONBOARDING);
        workflow1.setStatus(WorkflowStatus.IN_PROGRESS);
        workflow1.setInitiatedBy(adminUserId);
        workflow1.setInitiatedAt(LocalDateTime.now().minusDays(2));
        workflowInstanceRepository.save(workflow1);

        WorkflowInstance workflow2 = new WorkflowInstance();
        workflow2.setTemplateId(templateId);
        workflow2.setEmployeeName("Second Employee");
        workflow2.setEmployeeEmail("second@example.com");
        workflow2.setEmployeeRole("Designer");
        workflow2.setWorkflowType(WorkflowType.ONBOARDING);
        workflow2.setStatus(WorkflowStatus.IN_PROGRESS);
        workflow2.setInitiatedBy(adminUserId);
        workflow2.setInitiatedAt(LocalDateTime.now());
        workflowInstanceRepository.save(workflow2);

        // Act
        List<WorkflowInstance> results = workflowInstanceRepository
            .findByStatusOrderByInitiatedAtDesc(WorkflowStatus.IN_PROGRESS);

        // Assert
        assertFalse(results.isEmpty(), "Should find workflows");
        assertTrue(results.size() >= 2, "Should find at least 2 workflows");

        // Find our test workflows in results
        WorkflowInstance foundWorkflow2 = results.stream()
            .filter(w -> w.getEmployeeEmail().equals("second@example.com"))
            .findFirst()
            .orElse(null);
        WorkflowInstance foundWorkflow1 = results.stream()
            .filter(w -> w.getEmployeeEmail().equals("first@example.com"))
            .findFirst()
            .orElse(null);

        assertNotNull(foundWorkflow2);
        assertNotNull(foundWorkflow1);

        int index1 = results.indexOf(foundWorkflow1);
        int index2 = results.indexOf(foundWorkflow2);

        assertTrue(index2 < index1, "More recent workflow should appear first");
    }

    @Test
    void save_WithInvalidTemplateId_ThrowsException() {
        // Arrange
        UUID adminUserId = userRepository.findAll().get(0).getId();
        UUID nonExistentTemplateId = UUID.randomUUID();

        WorkflowInstance workflowInstance = new WorkflowInstance();
        workflowInstance.setTemplateId(nonExistentTemplateId);
        workflowInstance.setEmployeeName("Test Employee");
        workflowInstance.setEmployeeEmail("test@example.com");
        workflowInstance.setEmployeeRole("Developer");
        workflowInstance.setWorkflowType(WorkflowType.ONBOARDING);
        workflowInstance.setStatus(WorkflowStatus.INITIATED);
        workflowInstance.setInitiatedBy(adminUserId);
        workflowInstance.setInitiatedAt(LocalDateTime.now());

        // Act & Assert
        assertThrows(Exception.class, () -> {
            workflowInstanceRepository.save(workflowInstance);
            workflowInstanceRepository.flush();
        }, "Should throw foreign key constraint violation for invalid template_id");
    }

    @Test
    void save_WithInvalidInitiatedBy_ThrowsException() {
        // Arrange
        UUID templateId = workflowTemplateRepository.findAll().get(0).getId();
        UUID nonExistentUserId = UUID.randomUUID();

        WorkflowInstance workflowInstance = new WorkflowInstance();
        workflowInstance.setTemplateId(templateId);
        workflowInstance.setEmployeeName("Test Employee");
        workflowInstance.setEmployeeEmail("test@example.com");
        workflowInstance.setEmployeeRole("Developer");
        workflowInstance.setWorkflowType(WorkflowType.ONBOARDING);
        workflowInstance.setStatus(WorkflowStatus.INITIATED);
        workflowInstance.setInitiatedBy(nonExistentUserId);
        workflowInstance.setInitiatedAt(LocalDateTime.now());

        // Act & Assert
        assertThrows(Exception.class, () -> {
            workflowInstanceRepository.save(workflowInstance);
            workflowInstanceRepository.flush();
        }, "Should throw foreign key constraint violation for invalid initiated_by");
    }

    @Test
    void save_WithNullRequiredFields_ThrowsException() {
        // Arrange - missing employee_name
        UUID adminUserId = userRepository.findAll().get(0).getId();
        UUID templateId = workflowTemplateRepository.findAll().get(0).getId();

        WorkflowInstance workflowInstance = new WorkflowInstance();
        workflowInstance.setTemplateId(templateId);
        // employeeName is null
        workflowInstance.setEmployeeEmail("test@example.com");
        workflowInstance.setEmployeeRole("Developer");
        workflowInstance.setWorkflowType(WorkflowType.ONBOARDING);
        workflowInstance.setStatus(WorkflowStatus.INITIATED);
        workflowInstance.setInitiatedBy(adminUserId);

        // Act & Assert
        assertThrows(Exception.class, () -> {
            workflowInstanceRepository.save(workflowInstance);
            workflowInstanceRepository.flush();
        }, "Should throw NOT NULL constraint violation for missing required fields");
    }

    @Test
    void update_ModifiesUpdatedAtTimestamp() {
        // Arrange
        UUID adminUserId = userRepository.findAll().get(0).getId();
        UUID templateId = workflowTemplateRepository.findAll().get(0).getId();

        WorkflowInstance workflowInstance = new WorkflowInstance();
        workflowInstance.setTemplateId(templateId);
        workflowInstance.setEmployeeName("Update Test");
        workflowInstance.setEmployeeEmail("update.test@example.com");
        workflowInstance.setEmployeeRole("Developer");
        workflowInstance.setWorkflowType(WorkflowType.ONBOARDING);
        workflowInstance.setStatus(WorkflowStatus.INITIATED);
        workflowInstance.setInitiatedBy(adminUserId);
        workflowInstance.setInitiatedAt(LocalDateTime.now());

        WorkflowInstance saved = workflowInstanceRepository.save(workflowInstance);
        LocalDateTime originalUpdatedAt = saved.getUpdatedAt();

        // Wait a moment to ensure timestamp difference
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act - update the workflow
        saved.setStatus(WorkflowStatus.IN_PROGRESS);
        WorkflowInstance updated = workflowInstanceRepository.save(saved);

        // Assert
        assertNotEquals(originalUpdatedAt, updated.getUpdatedAt(),
            "Updated timestamp should change on update");
        assertEquals(WorkflowStatus.IN_PROGRESS, updated.getStatus());
    }
}
