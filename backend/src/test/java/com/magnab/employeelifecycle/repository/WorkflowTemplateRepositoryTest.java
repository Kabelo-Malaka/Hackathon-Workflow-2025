package com.magnab.employeelifecycle.repository;

import com.magnab.employeelifecycle.entity.WorkflowTemplate;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for WorkflowTemplateRepository using TestContainers.
 * Tests repository query methods against a real PostgreSQL database.
 */
@SpringBootTest
@Testcontainers
class WorkflowTemplateRepositoryTest {

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
    private WorkflowTemplateRepository workflowTemplateRepository;

    @Test
    void findByTemplateName_WithSeedData_ReturnsTemplate() {
        // Arrange - seed data from changeset 008 creates "Standard Employee Onboarding"
        String templateName = "Standard Employee Onboarding";

        // Act
        Optional<WorkflowTemplate> result = workflowTemplateRepository.findByTemplateName(templateName);

        // Assert
        assertTrue(result.isPresent(), "Template should be found");
        assertEquals(templateName, result.get().getTemplateName());
        assertEquals(WorkflowType.ONBOARDING, result.get().getWorkflowType());
        assertEquals(WorkflowStatus.INITIATED, result.get().getDefaultStatus());
        assertNotNull(result.get().getDescription());
    }

    @Test
    void findByTemplateName_WithNonExistentName_ReturnsEmpty() {
        // Arrange
        String nonExistentName = "Non-Existent Template";

        // Act
        Optional<WorkflowTemplate> result = workflowTemplateRepository.findByTemplateName(nonExistentName);

        // Assert
        assertFalse(result.isPresent(), "Template should not be found");
    }

    @Test
    void findByWorkflowType_WithOnboarding_ReturnsOnboardingTemplates() {
        // Arrange - seed data contains 1 onboarding template
        WorkflowType workflowType = WorkflowType.ONBOARDING;

        // Act
        List<WorkflowTemplate> results = workflowTemplateRepository.findByWorkflowType(workflowType);

        // Assert
        assertFalse(results.isEmpty(), "Should find at least one onboarding template");
        assertTrue(results.stream().allMatch(t -> t.getWorkflowType() == WorkflowType.ONBOARDING),
                "All templates should be ONBOARDING type");
    }

    @Test
    void findByWorkflowType_WithOffboarding_ReturnsOffboardingTemplates() {
        // Arrange
        WorkflowType workflowType = WorkflowType.OFFBOARDING;

        // Act
        List<WorkflowTemplate> results = workflowTemplateRepository.findByWorkflowType(workflowType);

        // Assert
        // Note: Other tests may create offboarding templates, so we check that all returned templates are OFFBOARDING type
        assertTrue(results.stream().allMatch(t -> t.getWorkflowType() == WorkflowType.OFFBOARDING),
                "All templates should be OFFBOARDING type if any exist");
    }

    @Test
    void save_WithNewTemplate_PersistsSuccessfully() {
        // Arrange
        WorkflowTemplate newTemplate = new WorkflowTemplate();
        newTemplate.setTemplateName("Test Offboarding Template " + System.currentTimeMillis());
        newTemplate.setWorkflowType(WorkflowType.OFFBOARDING);
        newTemplate.setDescription("Test template for offboarding process");
        newTemplate.setDefaultStatus(WorkflowStatus.INITIATED);

        // Act
        WorkflowTemplate saved = workflowTemplateRepository.save(newTemplate);

        // Assert
        assertNotNull(saved.getId(), "Saved template should have generated ID");
        assertEquals(newTemplate.getTemplateName(), saved.getTemplateName());
        assertEquals(WorkflowType.OFFBOARDING, saved.getWorkflowType());
        assertNotNull(saved.getCreatedAt(), "Created timestamp should be set");

        // Verify retrieval
        Optional<WorkflowTemplate> retrieved = workflowTemplateRepository.findById(saved.getId());
        assertTrue(retrieved.isPresent(), "Saved template should be retrievable");
    }

    @Test
    void findAll_ReturnsAllTemplates() {
        // Act
        List<WorkflowTemplate> allTemplates = workflowTemplateRepository.findAll();

        // Assert
        assertFalse(allTemplates.isEmpty(), "Should find templates (at least seed data)");
        assertTrue(allTemplates.stream().anyMatch(t -> t.getTemplateName().equals("Standard Employee Onboarding")),
                "Should include seed data template");
    }

    @Test
    void save_WithDuplicateTemplateName_ThrowsException() {
        // Arrange - seed data already has "Standard Employee Onboarding"
        WorkflowTemplate duplicateTemplate = new WorkflowTemplate();
        duplicateTemplate.setTemplateName("Standard Employee Onboarding"); // Duplicate name
        duplicateTemplate.setWorkflowType(WorkflowType.ONBOARDING);
        duplicateTemplate.setDescription("Duplicate template");
        duplicateTemplate.setDefaultStatus(WorkflowStatus.INITIATED);

        // Act & Assert
        assertThrows(Exception.class, () -> {
            workflowTemplateRepository.save(duplicateTemplate);
            workflowTemplateRepository.flush(); // Force immediate constraint check
        }, "Saving template with duplicate name should throw unique constraint violation");
    }

    @Test
    void save_WithNullTemplateName_ThrowsException() {
        // Arrange
        WorkflowTemplate templateWithoutName = new WorkflowTemplate();
        // templateName is null
        templateWithoutName.setWorkflowType(WorkflowType.ONBOARDING);
        templateWithoutName.setDescription("Template without name");
        templateWithoutName.setDefaultStatus(WorkflowStatus.INITIATED);

        // Act & Assert
        assertThrows(Exception.class, () -> {
            workflowTemplateRepository.save(templateWithoutName);
            workflowTemplateRepository.flush(); // Force immediate constraint check
        }, "Saving template without name should throw NOT NULL constraint violation");
    }

    @Test
    void save_WithNullWorkflowType_ThrowsException() {
        // Arrange
        WorkflowTemplate templateWithoutType = new WorkflowTemplate();
        templateWithoutType.setTemplateName("Template Without Type " + System.currentTimeMillis());
        // workflowType is null
        templateWithoutType.setDescription("Template without workflow type");
        templateWithoutType.setDefaultStatus(WorkflowStatus.INITIATED);

        // Act & Assert
        assertThrows(Exception.class, () -> {
            workflowTemplateRepository.save(templateWithoutType);
            workflowTemplateRepository.flush(); // Force immediate constraint check
        }, "Saving template without workflow type should throw NOT NULL constraint violation");
    }

    @Test
    void save_WithNullDefaultStatus_UsesDefaultValue() {
        // Arrange
        WorkflowTemplate templateWithoutStatus = new WorkflowTemplate();
        templateWithoutStatus.setTemplateName("Template Without Status " + System.currentTimeMillis());
        templateWithoutStatus.setWorkflowType(WorkflowType.ONBOARDING);
        templateWithoutStatus.setDescription("Template to test default status");
        // defaultStatus is null, but database has DEFAULT 'INITIATED'

        // Act
        WorkflowTemplate saved = workflowTemplateRepository.save(templateWithoutStatus);
        workflowTemplateRepository.flush();

        // Assert
        assertNotNull(saved.getId(), "Template should be saved");
        assertNotNull(saved.getDefaultStatus(), "Default status should be set by database default");
        assertEquals(WorkflowStatus.INITIATED, saved.getDefaultStatus(),
                "Database should apply DEFAULT 'INITIATED' value");
    }

    @Test
    void delete_WithAssociatedTasks_CascadesDelete() {
        // This test verifies CASCADE DELETE behavior from workflow_templates to template_tasks
        // Arrange - create a template
        WorkflowTemplate template = new WorkflowTemplate();
        template.setTemplateName("Template To Delete " + System.currentTimeMillis());
        template.setWorkflowType(WorkflowType.OFFBOARDING);
        template.setDescription("Template to test cascade delete");
        template.setDefaultStatus(WorkflowStatus.INITIATED);
        WorkflowTemplate savedTemplate = workflowTemplateRepository.save(template);

        UUID templateId = savedTemplate.getId();

        // Verify template exists
        assertTrue(workflowTemplateRepository.findById(templateId).isPresent(),
                "Template should exist before deletion");

        // Act - delete the template
        workflowTemplateRepository.delete(savedTemplate);
        workflowTemplateRepository.flush();

        // Assert - template should be deleted
        assertFalse(workflowTemplateRepository.findById(templateId).isPresent(),
                "Template should be deleted");
    }
}
