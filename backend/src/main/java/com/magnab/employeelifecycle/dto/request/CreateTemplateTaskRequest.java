package com.magnab.employeelifecycle.dto.request;

import com.magnab.employeelifecycle.enums.UserRole;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Request DTO for creating a template task.
 * Used as nested DTO within CreateTemplateRequest.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateTemplateTaskRequest {

    @NotBlank(message = "Task name is required")
    @Size(max = 255, message = "Task name must not exceed 255 characters")
    private String taskName;

    private String description;

    @NotNull(message = "Assigned role is required")
    private UserRole assignedRole;

    @NotNull(message = "Sequence order is required")
    @Min(value = 1, message = "Sequence order must be greater than 0")
    private Integer sequenceOrder;

    private Boolean isParallel = false;

    private UUID dependencyTaskId;
}
