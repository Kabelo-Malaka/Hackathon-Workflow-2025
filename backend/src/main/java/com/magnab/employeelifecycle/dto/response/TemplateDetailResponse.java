package com.magnab.employeelifecycle.dto.response;

import com.magnab.employeelifecycle.enums.WorkflowType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for complete template details.
 * Used in POST /api/templates, GET /api/templates/{id}, PUT /api/templates/{id}.
 * Contains all template fields including nested task details.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TemplateDetailResponse {

    private UUID id;
    private String name;
    private String description;
    private WorkflowType type;
    private Boolean isActive;
    private List<TaskDetailResponse> tasks;
    private LocalDateTime createdAt;
    private UUID createdBy;
    private LocalDateTime updatedAt;
    private UUID updatedBy;
}
