package com.magnab.employeelifecycle.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO containing the result of a single task assignment.
 * Returned when tasks are automatically assigned to users based on role and workload.
 */
@Data
public class TaskAssignmentResult {

    private UUID taskInstanceId;
    private UUID assignedUserId;
    private String assignedUserEmail;
    private String taskName;
    private LocalDateTime dueDate;
}
