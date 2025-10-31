package com.magnab.employeelifecycle.dto.response;

import com.magnab.employeelifecycle.enums.WorkflowStatus;
import lombok.Data;

import java.util.UUID;

/**
 * Response DTO containing workflow state summary with task counts by status.
 * Returned after workflow status updates to provide current workflow state.
 */
@Data
public class WorkflowStateSummary {

    private UUID workflowInstanceId;
    private WorkflowStatus status;
    private Integer totalTasks;
    private Integer tasksCompleted;
    private Integer tasksInProgress;
    private Integer tasksBlocked;
    private Integer tasksNotStarted;
}
