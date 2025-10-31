package com.magnab.employeelifecycle.dto.response;

import com.magnab.employeelifecycle.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for template task details.
 * Used as nested DTO within TemplateDetailResponse.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskDetailResponse {

    private UUID id;
    private String taskName;
    private String description;
    private UserRole assignedRole;
    private Integer sequenceOrder;
    private Boolean isParallel;
    private UUID dependencyTaskId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
