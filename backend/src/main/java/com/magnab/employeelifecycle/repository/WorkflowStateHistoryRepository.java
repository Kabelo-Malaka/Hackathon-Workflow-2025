package com.magnab.employeelifecycle.repository;

import com.magnab.employeelifecycle.entity.WorkflowStateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for WorkflowStateHistory entity.
 * Provides CRUD operations and custom queries for workflow state history audit trail.
 */
@Repository
public interface WorkflowStateHistoryRepository extends JpaRepository<WorkflowStateHistory, UUID> {

    /**
     * Find all state history records for a specific workflow, ordered chronologically.
     * Leverages composite index (workflow_instance_id, changed_at) for audit trail queries.
     * Used for displaying workflow status change timeline.
     */
    List<WorkflowStateHistory> findByWorkflowInstanceIdOrderByChangedAtAsc(UUID workflowInstanceId);

    /**
     * Find all state history records for a specific workflow, reverse chronological.
     * Used for showing most recent status changes first.
     */
    List<WorkflowStateHistory> findByWorkflowInstanceIdOrderByChangedAtDesc(UUID workflowInstanceId);
}
