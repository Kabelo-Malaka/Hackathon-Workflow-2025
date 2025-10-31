package com.magnab.employeelifecycle.dto.response;

import com.magnab.employeelifecycle.enums.WorkflowStatus;
import com.magnab.employeelifecycle.enums.WorkflowType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for detailed workflow view.
 * Contains complete workflow information including metadata, custom fields, tasks, and state history.
 */
@Data
public class WorkflowDetailResponse {

    private UUID id;
    private String employeeName;
    private String employeeEmail;
    private String employeeRole;
    private WorkflowType workflowType;
    private WorkflowStatus status;
    private LocalDateTime initiatedAt;
    private LocalDateTime completedAt;
    private UUID initiatedBy;
    private Map<String, Object> customFieldValues;
    private List<TaskInstanceSummary> tasks;
    private List<WorkflowStateHistoryEntry> stateHistory;
}
