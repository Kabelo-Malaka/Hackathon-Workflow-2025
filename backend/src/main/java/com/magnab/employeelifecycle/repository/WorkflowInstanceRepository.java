package com.magnab.employeelifecycle.repository;

import com.magnab.employeelifecycle.entity.WorkflowInstance;
import com.magnab.employeelifecycle.enums.WorkflowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for WorkflowInstance entity.
 * Provides CRUD operations and custom queries for workflow instance management.
 */
@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, UUID> {

    /**
     * Find workflow instance by employee email.
     * Useful for checking if an employee already has an active workflow.
     */
    List<WorkflowInstance> findByEmployeeEmail(String employeeEmail);

    /**
     * Find workflow instances by status.
     * Used for dashboard filtering (e.g., show all active workflows).
     */
    List<WorkflowInstance> findByStatus(WorkflowStatus status);

    /**
     * Find workflow instances by template ID.
     * Useful for analyzing workflow performance by template.
     */
    List<WorkflowInstance> findByTemplateId(UUID templateId);

    /**
     * Find workflow instances by status, ordered by initiation date.
     * Leverages composite index (status, initiated_at) for dashboard queries.
     */
    List<WorkflowInstance> findByStatusOrderByInitiatedAtDesc(WorkflowStatus status);
}
