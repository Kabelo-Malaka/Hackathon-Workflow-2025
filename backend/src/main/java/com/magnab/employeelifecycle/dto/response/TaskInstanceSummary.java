package com.magnab.employeelifecycle.dto.response;

import com.magnab.employeelifecycle.enums.TaskStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Nested DTO for task instance information within workflow detail response.
 * Contains task instance details including assignment and completion information.
 */
@Data
public class TaskInstanceSummary {

    private UUID id;
    private String taskName;
    private TaskStatus status;
    private UUID assignedUserId;
    private String assignedUserName;
    private String assignedRole;
    private Boolean isVisible;
    private LocalDateTime dueDate;
    private LocalDateTime completedAt;
    private UUID completedBy;
}
