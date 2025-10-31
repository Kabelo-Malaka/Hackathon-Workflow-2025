package com.magnab.employeelifecycle.dto.response;

import com.magnab.employeelifecycle.enums.TaskStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO containing the result of a task status update.
 * Returned when task status is updated to provide updated task details.
 */
@Data
public class TaskStatusUpdate {

    private UUID taskInstanceId;
    private String taskName;
    private TaskStatus status;
    private LocalDateTime completedAt;
    private UUID completedBy;
}
