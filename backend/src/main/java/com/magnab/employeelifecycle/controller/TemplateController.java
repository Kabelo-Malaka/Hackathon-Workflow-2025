package com.magnab.employeelifecycle.controller;

import com.magnab.employeelifecycle.dto.request.CreateTemplateRequest;
import com.magnab.employeelifecycle.dto.request.UpdateTemplateRequest;
import com.magnab.employeelifecycle.dto.response.TemplateDetailResponse;
import com.magnab.employeelifecycle.dto.response.TemplateSummaryResponse;
import com.magnab.employeelifecycle.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for workflow template management.
 * Provides CRUD endpoints for creating and managing workflow templates.
 * All endpoints require HR_ADMIN or ADMINISTRATOR role.
 */
@RestController
@RequestMapping("/api/templates")
@PreAuthorize("hasAnyAuthority('ROLE_HR_ADMIN', 'ROLE_ADMINISTRATOR')")
@Tag(name = "Template Management", description = "APIs for managing workflow templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    /**
     * Create a new workflow template.
     *
     * @param request Request containing template data
     * @return Created template with 201 status
     */
    @PostMapping
    @Operation(summary = "Create a new workflow template",
               description = "Creates a new workflow template with associated tasks. " +
                           "Validates: minimum 1 task, unique sequence orders for non-parallel tasks, " +
                           "valid dependencies, no circular dependencies. Automatically normalizes sequence gaps.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Template created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data. Possible validation errors: " +
                    "template must have at least one task, " +
                    "non-parallel tasks must have unique sequence orders, " +
                    "dependency references non-existent task, " +
                    "circular dependency detected"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - requires HR_ADMIN or ADMINISTRATOR role")
    })
    public ResponseEntity<TemplateDetailResponse> createTemplate(
            @Valid @RequestBody CreateTemplateRequest request) {

        TemplateDetailResponse response = templateService.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all workflow templates.
     *
     * @return List of template summaries
     */
    @GetMapping
    @Operation(summary = "Get all workflow templates",
               description = "Retrieves a list of all workflow templates with summary information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Templates retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - requires HR_ADMIN or ADMINISTRATOR role")
    })
    public ResponseEntity<List<TemplateSummaryResponse>> getAllTemplates() {
        List<TemplateSummaryResponse> templates = templateService.getAllTemplates();
        return ResponseEntity.ok(templates);
    }

    /**
     * Get a single template by ID.
     *
     * @param id Template ID
     * @return Complete template details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get template by ID",
               description = "Retrieves complete details of a specific workflow template including all tasks")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Template retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - requires HR_ADMIN or ADMINISTRATOR role"),
        @ApiResponse(responseCode = "404", description = "Template not found")
    })
    public ResponseEntity<TemplateDetailResponse> getTemplateById(
            @Parameter(description = "Template ID") @PathVariable UUID id) {

        TemplateDetailResponse response = templateService.getTemplateById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing workflow template.
     *
     * @param id      Template ID
     * @param request Request containing updated template data
     * @return Updated template
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update a workflow template",
               description = "Updates an existing workflow template and replaces all associated tasks. " +
                           "Validates: minimum 1 task, unique sequence orders for non-parallel tasks, " +
                           "valid dependencies, no circular dependencies. Automatically normalizes sequence gaps.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Template updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data. Possible validation errors: " +
                    "template must have at least one task, " +
                    "non-parallel tasks must have unique sequence orders, " +
                    "dependency references non-existent task, " +
                    "circular dependency detected"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - requires HR_ADMIN or ADMINISTRATOR role"),
        @ApiResponse(responseCode = "404", description = "Template not found")
    })
    public ResponseEntity<TemplateDetailResponse> updateTemplate(
            @Parameter(description = "Template ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateTemplateRequest request) {

        TemplateDetailResponse response = templateService.updateTemplate(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a workflow template (soft delete).
     *
     * @param id Template ID
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a workflow template",
               description = "Soft deletes a workflow template by setting is_active to false")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Template deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - requires HR_ADMIN or ADMINISTRATOR role"),
        @ApiResponse(responseCode = "404", description = "Template not found"),
        @ApiResponse(responseCode = "409", description = "Conflict - template is in use by active workflows")
    })
    public ResponseEntity<Void> deleteTemplate(
            @Parameter(description = "Template ID") @PathVariable UUID id) {

        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }
}
