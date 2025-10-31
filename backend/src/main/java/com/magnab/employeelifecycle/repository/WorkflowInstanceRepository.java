package com.magnab.employeelifecycle.repository;

import com.magnab.employeelifecycle.entity.WorkflowInstance;
import com.magnab.employeelifecycle.enums.WorkflowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Find all workflows with optional filters.
     * Used by HR_ADMIN to view all workflows with pagination, filtering, and sorting.
     *
     * @param status Optional status filter
     * @param workflowType Optional workflow type filter
     * @param employeeNameSearch Optional case-insensitive employee name search
     * @param pageable Pagination and sorting parameters
     * @return Page of workflow instances matching filters
     */
    @Query("SELECT w FROM WorkflowInstance w " +
           "WHERE (:status IS NULL OR w.status = :status) " +
           "AND (:workflowType IS NULL OR w.workflowType = :workflowType) " +
           "AND (:employeeNameSearch IS NULL OR LOWER(w.employeeName) LIKE LOWER(CONCAT('%', :employeeNameSearch, '%')))")
    Page<WorkflowInstance> findAllByFilters(
        @Param("status") WorkflowStatus status,
        @Param("workflowType") String workflowType,
        @Param("employeeNameSearch") String employeeNameSearch,
        Pageable pageable
    );

    /**
     * Find workflows where user has assigned tasks, with optional filters.
     * Used by non-admin users to view only workflows they're involved in.
     *
     * @param userId User ID to filter by
     * @param status Optional status filter
     * @param workflowType Optional workflow type filter
     * @param employeeNameSearch Optional case-insensitive employee name search
     * @param pageable Pagination and sorting parameters
     * @return Page of workflow instances where user has assigned tasks
     */
    @Query("SELECT DISTINCT w FROM WorkflowInstance w " +
           "JOIN TaskInstance t ON t.workflowInstanceId = w.id " +
           "WHERE t.assignedUserId = :userId " +
           "AND (:status IS NULL OR w.status = :status) " +
           "AND (:workflowType IS NULL OR w.workflowType = :workflowType) " +
           "AND (:employeeNameSearch IS NULL OR LOWER(w.employeeName) LIKE LOWER(CONCAT('%', :employeeNameSearch, '%')))")
    Page<WorkflowInstance> findByUserHasAssignedTasks(
        @Param("userId") UUID userId,
        @Param("status") WorkflowStatus status,
        @Param("workflowType") String workflowType,
        @Param("employeeNameSearch") String employeeNameSearch,
        Pageable pageable
    );
}
