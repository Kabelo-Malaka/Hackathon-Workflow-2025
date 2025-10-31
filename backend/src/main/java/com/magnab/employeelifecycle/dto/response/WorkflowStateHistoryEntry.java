package com.magnab.employeelifecycle.dto.response;

import com.magnab.employeelifecycle.enums.WorkflowStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Nested DTO for workflow state history information within workflow detail response.
 * Tracks state transitions for audit trail.
 */
@Data
public class WorkflowStateHistoryEntry {

    private UUID id;
    private WorkflowStatus previousStatus;
    private WorkflowStatus newStatus;
    private UUID changedBy;
    private String changedByName;
    private LocalDateTime changedAt;
    private String notes;
}
