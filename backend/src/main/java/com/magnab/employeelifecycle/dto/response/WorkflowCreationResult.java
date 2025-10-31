package com.magnab.employeelifecycle.dto.response;

import lombok.Data;

import java.util.UUID;

/**
 * Response DTO containing the result of workflow instance creation.
 * Provides summary information about the newly created workflow.
 */
@Data
public class WorkflowCreationResult {

    private UUID workflowInstanceId;
    private Integer totalTasks;
    private Integer immediateTasksCount;
}
