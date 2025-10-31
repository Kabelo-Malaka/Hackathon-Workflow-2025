package com.magnab.employeelifecycle.dto.response;

import com.magnab.employeelifecycle.enums.WorkflowType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for template summary information.
 * Used in GET /api/templates (list all templates).
 * Contains essential fields without nested task details.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TemplateSummaryResponse {

    private UUID id;
    private String name;
    private WorkflowType type;
    private Boolean isActive;
    private Integer taskCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
