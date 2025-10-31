package com.magnab.employeelifecycle.repository;

import com.magnab.employeelifecycle.entity.WorkflowInstance;
import com.magnab.employeelifecycle.entity.WorkflowStateHistory;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for WorkflowStateHistoryRepository using TestContainers.
 * Tests audit trail functionality and cascade delete behavior.
 */
@SpringBootTest
@Testcontainers
class WorkflowStateHistoryRepositoryTest {

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
    private WorkflowStateHistoryRepository workflowStateHistoryRepository;

    @Autowired
    private WorkflowInstanceRepository workflowInstanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkflowTemplateRepository workflowTemplateRepository;

    @Test
    void save_WithAllRequiredFields_PersistsSuccessfully() {
        // Arrange
        UUID adminUserId = userRepository.findAll().get(0).getId();
        UUID templateId = workflowTemplateRepository.findAll().get(0).getId();

        WorkflowInstance workflowInstance = createTestWorkflowInstance(adminUserId, templateId);
        WorkflowInstance savedWorkflow = workflowInstanceRepository.save(workflowInstance);

        WorkflowStateHistory history = new WorkflowStateHistory();
        history.setWorkflowInstanceId(savedWorkflow.getId());
        history.setPreviousStatus(WorkflowStatus.INITIATED);
        history.setNewStatus(WorkflowStatus.IN_PROGRESS);
        history.setChangedBy(adminUserId);
        history.setChangedAt(LocalDateTime.now());
        history.setNotes("Workflow started by admin");

        // Act
        WorkflowStateHistory saved = workflowStateHistoryRepository.save(history);
        workflowStateHistoryRepository.flush();

        // Assert
        assertNotNull(saved.getId(), "State history should have generated ID");
        assertEquals(WorkflowStatus.INITIATED, saved.getPreviousStatus());
        assertEquals(WorkflowStatus.IN_PROGRESS, saved.getNewStatus());
        assertEquals(adminUserId, saved.getChangedBy());
        assertEquals("Workflow started by admin", saved.getNotes());
        assertNotNull(saved.getChangedAt());
    }

    @Test
    void save_WithoutNotes_AllowsNull() {
        // Arrange
        UUID adminUserId = userRepository.findAll().get(0).getId();
        UUID templateId = workflowTemplateRepository.findAll().get(0).getId();

        WorkflowInstance workflowInstance = createTestWorkflowInstance(adminUserId, templateId);
        WorkflowInstance savedWorkflow = workflowInstanceRepository.save(workflowInstance);

        WorkflowStateHistory history = new WorkflowStateHistory();
        history.setWorkflowInstanceId(savedWorkflow.getId());
        history.setPreviousStatus(WorkflowStatus.IN_PROGRESS);
        history.setNewStatus(WorkflowStatus.COMPLETED);
        history.setChangedBy(adminUserId);
        history.setChangedAt(LocalDateTime.now());
        // notes is null

        // Act
        WorkflowStateHistory saved = workflowStateHistoryRepository.save(history);
        workflowStateHistoryRepository.flush();

        // Assert
        assertNotNull(saved.getId());
        assertNull(saved.getNotes(), "Notes field can be null");
    }

    @Test
    void findByWorkflowInstanceIdOrderByChangedAtAsc_ReturnsChronologicalHistory() {
        // Arrange
        UUID adminUserId = userRepository.findAll().get(0).getId();
        UUID templateId = workflowTemplateRepository.findAll().get(0).getId();

        WorkflowInstance workflowInstance = createTestWorkflowInstance(adminUserId, templateId);
        WorkflowInstance savedWorkflow = workflowInstanceRepository.save(workflowInstance);

        // Create multiple state changes
        WorkflowStateHistory history1 = new WorkflowStateHistory();
        history1.setWorkflowInstanceId(savedWorkflow.getId());
        history1.setPreviousStatus(WorkflowStatus.INITIATED);
        history1.setNewStatus(WorkflowStatus.IN_PROGRESS);
        history1.setChangedBy(adminUserId);
        history1.setChangedAt(LocalDateTime.now().minusDays(2));
        workflowStateHistoryRepository.save(history1);

        WorkflowStateHistory history2 = new WorkflowStateHistory();
        history2.setWorkflowInstanceId(savedWorkflow.getId());
        history2.setPreviousStatus(WorkflowStatus.IN_PROGRESS);
        history2.setNewStatus(WorkflowStatus.BLOCKED);
        history2.setChangedBy(adminUserId);
        history2.setChangedAt(LocalDateTime.now().minusDays(1));
        workflowStateHistoryRepository.save(history2);

        WorkflowStateHistory history3 = new WorkflowStateHistory();
        history3.setWorkflowInstanceId(savedWorkflow.getId());
        history3.setPreviousStatus(WorkflowStatus.BLOCKED);
        history3.setNewStatus(WorkflowStatus.IN_PROGRESS);
        history3.setChangedBy(adminUserId);
        history3.setChangedAt(LocalDateTime.now());
        workflowStateHistoryRepository.save(history3);

        // Act
        List<WorkflowStateHistory> results = workflowStateHistoryRepository
            .findByWorkflowInstanceIdOrderByChangedAtAsc(savedWorkflow.getId());

        // Assert
        assertEquals(3, results.size(), "Should find 3 state history records");

        // Verify chronological order (oldest first)
        assertEquals(WorkflowStatus.INITIATED, results.get(0).getPreviousStatus());
        assertEquals(WorkflowStatus.IN_PROGRESS, results.get(0).getNewStatus());

        assertEquals(WorkflowStatus.IN_PROGRESS, results.get(1).getPreviousStatus());
        assertEquals(WorkflowStatus.BLOCKED, results.get(1).getNewStatus());

        assertEquals(WorkflowStatus.BLOCKED, results.get(2).getPreviousStatus());
        assertEquals(WorkflowStatus.IN_PROGRESS, results.get(2).getNewStatus());

        // Verify timestamps are in ascending order
        assertTrue(results.get(0).getChangedAt().isBefore(results.get(1).getChangedAt()));
        assertTrue(results.get(1).getChangedAt().isBefore(results.get(2).getChangedAt()));
    }

    @Test
    void findByWorkflowInstanceIdOrderByChangedAtDesc_ReturnsReverseChronologicalHistory() {
        // Arrange
        UUID adminUserId = userRepository.findAll().get(0).getId();
        UUID templateId = workflowTemplateRepository.findAll().get(0).getId();

        WorkflowInstance workflowInstance = createTestWorkflowInstance(adminUserId, templateId);
        WorkflowInstance savedWorkflow = workflowInstanceRepository.save(workflowInstance);

        WorkflowStateHistory history1 = new WorkflowStateHistory();
        history1.setWorkflowInstanceId(savedWorkflow.getId());
        history1.setPreviousStatus(WorkflowStatus.INITIATED);
        history1.setNewStatus(WorkflowStatus.IN_PROGRESS);
        history1.setChangedBy(adminUserId);
        history1.setChangedAt(LocalDateTime.now().minusDays(1));
        workflowStateHistoryRepository.save(history1);

        WorkflowStateHistory history2 = new WorkflowStateHistory();
        history2.setWorkflowInstanceId(savedWorkflow.getId());
        history2.setPreviousStatus(WorkflowStatus.IN_PROGRESS);
        history2.setNewStatus(WorkflowStatus.COMPLETED);
        history2.setChangedBy(adminUserId);
        history2.setChangedAt(LocalDateTime.now());
        workflowStateHistoryRepository.save(history2);

        // Act
        List<WorkflowStateHistory> results = workflowStateHistoryRepository
            .findByWorkflowInstanceIdOrderByChangedAtDesc(savedWorkflow.getId());

        // Assert
        assertEquals(2, results.size(), "Should find 2 state history records");

        // Verify reverse chronological order (newest first)
        assertEquals(WorkflowStatus.IN_PROGRESS, results.get(0).getPreviousStatus());
        assertEquals(WorkflowStatus.COMPLETED, results.get(0).getNewStatus());

        assertEquals(WorkflowStatus.INITIATED, results.get(1).getPreviousStatus());
        assertEquals(WorkflowStatus.IN_PROGRESS, results.get(1).getNewStatus());

        // Verify timestamps are in descending order
        assertTrue(results.get(0).getChangedAt().isAfter(results.get(1).getChangedAt()));
    }

    @Test
    void delete_WhenWorkflowDeleted_CascadesDelete() {
        // Arrange
        UUID adminUserId = userRepository.findAll().get(0).getId();
        UUID templateId = workflowTemplateRepository.findAll().get(0).getId();

        WorkflowInstance workflowInstance = createTestWorkflowInstance(adminUserId, templateId);
        WorkflowInstance savedWorkflow = workflowInstanceRepository.save(workflowInstance);

        WorkflowStateHistory history = new WorkflowStateHistory();
        history.setWorkflowInstanceId(savedWorkflow.getId());
        history.setPreviousStatus(WorkflowStatus.INITIATED);
        history.setNewStatus(WorkflowStatus.IN_PROGRESS);
        history.setChangedBy(adminUserId);
        history.setChangedAt(LocalDateTime.now());
        WorkflowStateHistory savedHistory = workflowStateHistoryRepository.save(history);
        UUID historyId = savedHistory.getId();

        // Verify history exists
        assertTrue(workflowStateHistoryRepository.findById(historyId).isPresent(),
            "State history should exist before workflow deletion");

        // Act - delete the workflow
        workflowInstanceRepository.delete(savedWorkflow);
        workflowInstanceRepository.flush();

        // Assert - history should be deleted due to CASCADE
        assertFalse(workflowStateHistoryRepository.findById(historyId).isPresent(),
            "State history should be deleted when workflow is deleted (CASCADE)");
    }

    @Test
    void save_WithInvalidWorkflowInstanceId_ThrowsException() {
        // Arrange
        UUID adminUserId = userRepository.findAll().get(0).getId();
        UUID nonExistentWorkflowId = UUID.randomUUID();

        WorkflowStateHistory history = new WorkflowStateHistory();
        history.setWorkflowInstanceId(nonExistentWorkflowId);
        history.setPreviousStatus(WorkflowStatus.INITIATED);
        history.setNewStatus(WorkflowStatus.IN_PROGRESS);
        history.setChangedBy(adminUserId);
        history.setChangedAt(LocalDateTime.now());

        // Act & Assert
        assertThrows(Exception.class, () -> {
            workflowStateHistoryRepository.save(history);
            workflowStateHistoryRepository.flush();
        }, "Should throw foreign key constraint violation for invalid workflow_instance_id");
    }

    @Test
    void save_WithInvalidChangedBy_ThrowsException() {
        // Arrange
        UUID templateId = workflowTemplateRepository.findAll().get(0).getId();
        UUID adminUserId = userRepository.findAll().get(0).getId();

        WorkflowInstance workflowInstance = createTestWorkflowInstance(adminUserId, templateId);
        WorkflowInstance savedWorkflow = workflowInstanceRepository.save(workflowInstance);

        UUID nonExistentUserId = UUID.randomUUID();

        WorkflowStateHistory history = new WorkflowStateHistory();
        history.setWorkflowInstanceId(savedWorkflow.getId());
        history.setPreviousStatus(WorkflowStatus.INITIATED);
        history.setNewStatus(WorkflowStatus.IN_PROGRESS);
        history.setChangedBy(nonExistentUserId);
        history.setChangedAt(LocalDateTime.now());

        // Act & Assert
        assertThrows(Exception.class, () -> {
            workflowStateHistoryRepository.save(history);
            workflowStateHistoryRepository.flush();
        }, "Should throw foreign key constraint violation for invalid changed_by");
    }

    @Test
    void save_WithNullRequiredFields_ThrowsException() {
        // Arrange
        UUID adminUserId = userRepository.findAll().get(0).getId();
        UUID templateId = workflowTemplateRepository.findAll().get(0).getId();

        WorkflowInstance workflowInstance = createTestWorkflowInstance(adminUserId, templateId);
        WorkflowInstance savedWorkflow = workflowInstanceRepository.save(workflowInstance);

        WorkflowStateHistory history = new WorkflowStateHistory();
        history.setWorkflowInstanceId(savedWorkflow.getId());
        history.setPreviousStatus(WorkflowStatus.INITIATED);
        // newStatus is null (required field)
        history.setChangedBy(adminUserId);
        history.setChangedAt(LocalDateTime.now());

        // Act & Assert
        assertThrows(Exception.class, () -> {
            workflowStateHistoryRepository.save(history);
            workflowStateHistoryRepository.flush();
        }, "Should throw NOT NULL constraint violation for missing required fields");
    }

    @Test
    void save_MultipleHistoryRecords_TrackFullAuditTrail() {
        // Arrange
        UUID adminUserId = userRepository.findAll().get(0).getId();
        UUID templateId = workflowTemplateRepository.findAll().get(0).getId();

        WorkflowInstance workflowInstance = createTestWorkflowInstance(adminUserId, templateId);
        WorkflowInstance savedWorkflow = workflowInstanceRepository.save(workflowInstance);

        // Simulate complete workflow lifecycle
        LocalDateTime baseTime = LocalDateTime.now().minusDays(10);

        WorkflowStateHistory[] transitions = {
            createStateHistory(savedWorkflow.getId(), WorkflowStatus.INITIATED, WorkflowStatus.IN_PROGRESS,
                adminUserId, baseTime, "Workflow started"),
            createStateHistory(savedWorkflow.getId(), WorkflowStatus.IN_PROGRESS, WorkflowStatus.BLOCKED,
                adminUserId, baseTime.plusDays(2), "Waiting for approval"),
            createStateHistory(savedWorkflow.getId(), WorkflowStatus.BLOCKED, WorkflowStatus.IN_PROGRESS,
                adminUserId, baseTime.plusDays(4), "Approval received"),
            createStateHistory(savedWorkflow.getId(), WorkflowStatus.IN_PROGRESS, WorkflowStatus.COMPLETED,
                adminUserId, baseTime.plusDays(8), "All tasks completed")
        };

        for (WorkflowStateHistory transition : transitions) {
            workflowStateHistoryRepository.save(transition);
        }
        workflowStateHistoryRepository.flush();

        // Act
        List<WorkflowStateHistory> auditTrail = workflowStateHistoryRepository
            .findByWorkflowInstanceIdOrderByChangedAtAsc(savedWorkflow.getId());

        // Assert
        assertEquals(4, auditTrail.size(), "Should have complete audit trail with 4 transitions");

        // Verify the sequence of state transitions
        assertEquals(WorkflowStatus.INITIATED, auditTrail.get(0).getPreviousStatus());
        assertEquals(WorkflowStatus.IN_PROGRESS, auditTrail.get(0).getNewStatus());

        assertEquals(WorkflowStatus.IN_PROGRESS, auditTrail.get(1).getPreviousStatus());
        assertEquals(WorkflowStatus.BLOCKED, auditTrail.get(1).getNewStatus());

        assertEquals(WorkflowStatus.BLOCKED, auditTrail.get(2).getPreviousStatus());
        assertEquals(WorkflowStatus.IN_PROGRESS, auditTrail.get(2).getNewStatus());

        assertEquals(WorkflowStatus.IN_PROGRESS, auditTrail.get(3).getPreviousStatus());
        assertEquals(WorkflowStatus.COMPLETED, auditTrail.get(3).getNewStatus());

        // Verify notes are captured
        assertEquals("Workflow started", auditTrail.get(0).getNotes());
        assertEquals("All tasks completed", auditTrail.get(3).getNotes());
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

    private WorkflowStateHistory createStateHistory(UUID workflowInstanceId, WorkflowStatus previousStatus,
                                                     WorkflowStatus newStatus, UUID changedBy,
                                                     LocalDateTime changedAt, String notes) {
        WorkflowStateHistory history = new WorkflowStateHistory();
        history.setWorkflowInstanceId(workflowInstanceId);
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setChangedBy(changedBy);
        history.setChangedAt(changedAt);
        history.setNotes(notes);
        return history;
    }
}
