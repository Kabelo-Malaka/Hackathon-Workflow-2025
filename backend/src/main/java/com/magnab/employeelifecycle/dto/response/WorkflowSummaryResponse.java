package com.magnab.employeelifecycle.dto.response;

import com.magnab.employeelifecycle.enums.WorkflowStatus;
import com.magnab.employeelifecycle.enums.WorkflowType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for workflow list view (summary information).
 * Contains essential workflow information for display in lists and tables.
 */
@Data
public class WorkflowSummaryResponse {

    private UUID id;
    private String employeeName;
    private WorkflowType workflowType;
    private WorkflowStatus status;
    private LocalDateTime initiatedAt;
    private Integer totalTasks;
    private Integer completedTasks;
}
