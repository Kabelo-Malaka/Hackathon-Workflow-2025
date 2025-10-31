package com.magnab.employeelifecycle.repository;

import com.magnab.employeelifecycle.entity.TaskInstance;
import com.magnab.employeelifecycle.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for TaskInstance entity.
 * Provides CRUD operations and custom queries for task instance management.
 */
@Repository
public interface TaskInstanceRepository extends JpaRepository<TaskInstance, UUID> {

    /**
     * Find all tasks for a specific workflow instance.
     * Used for displaying workflow progress and task lists.
     */
    List<TaskInstance> findByWorkflowInstanceId(UUID workflowInstanceId);

    /**
     * Find tasks assigned to a specific user.
     * Used for user task queues and "My Tasks" views.
     */
    List<TaskInstance> findByAssignedUserId(UUID assignedUserId);

    /**
     * Find tasks by status.
     * Used for filtering tasks by completion status.
     */
    List<TaskInstance> findByStatus(TaskStatus status);

    /**
     * Find tasks for a specific user with a specific status.
     * Leverages composite index (assigned_user_id, status) for user dashboard queries.
     */
    List<TaskInstance> findByAssignedUserIdAndStatus(UUID assignedUserId, TaskStatus status);

    /**
     * Find tasks in a workflow with a specific status.
     * Leverages composite index (workflow_instance_id, status) for workflow progress tracking.
     */
    List<TaskInstance> findByWorkflowInstanceIdAndStatus(UUID workflowInstanceId, TaskStatus status);

    /**
     * Find visible tasks for a workflow instance.
     * Used for conditional logic - only show tasks that should be visible to users.
     */
    List<TaskInstance> findByWorkflowInstanceIdAndIsVisible(UUID workflowInstanceId, Boolean isVisible);
}
