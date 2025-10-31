package com.magnab.employeelifecycle.dto.response;

import com.magnab.employeelifecycle.enums.WorkflowStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for workflow initiation endpoint.
 * Contains workflow instance details and task creation/assignment summary.
 */
@Data
public class WorkflowInitiationResponse {

    private UUID workflowInstanceId;
    private String employeeName;
    private WorkflowStatus status;
    private Integer totalTasksCreated;
    private Integer tasksAssigned;
    private LocalDateTime initiatedAt;
}
