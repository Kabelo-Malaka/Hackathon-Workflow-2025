package com.magnab.employeelifecycle.dto.request;

import com.magnab.employeelifecycle.enums.WorkflowType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Request DTO for updating an existing workflow template.
 * Replaces the entire template structure (cascade update to tasks).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTemplateRequest {

    @NotBlank(message = "Template name is required")
    @Size(max = 255, message = "Template name must not exceed 255 characters")
    private String name;

    private String description;

    @NotNull(message = "Template type is required")
    private WorkflowType type;

    @NotNull(message = "isActive flag is required")
    private Boolean isActive;

    @NotNull(message = "Tasks are required")
    @Valid
    private List<CreateTemplateTaskRequest> tasks;
}
