package com.magnab.employeelifecycle.controller;

import com.magnab.employeelifecycle.dto.request.EmployeeDetails;
import com.magnab.employeelifecycle.dto.request.InitiateWorkflowRequest;
import com.magnab.employeelifecycle.dto.response.TaskAssignmentResult;
import com.magnab.employeelifecycle.dto.response.WorkflowCreationResult;
import com.magnab.employeelifecycle.dto.response.WorkflowInitiationResponse;
import com.magnab.employeelifecycle.entity.User;
import com.magnab.employeelifecycle.enums.WorkflowStatus;
import com.magnab.employeelifecycle.exception.UnauthorizedException;
import com.magnab.employeelifecycle.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for workflow instance operations.
 * Provides endpoints for initiating and managing employee onboarding/offboarding workflows.
 */
@RestController
@RequestMapping("/api/workflows")
@Tag(name = "Workflows", description = "Workflow instance management endpoints")
public class WorkflowController {

    private static final Logger log = LoggerFactory.getLogger(WorkflowController.class);
    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    /**
     * Initiates a new onboarding or offboarding workflow for an employee.
     * Only HR_ADMIN role can initiate workflows.
     *
     * @param request Workflow initiation request containing template ID and employee details
     * @return WorkflowInitiationResponse with workflow ID and task summary
     */
    @Operation(
            summary = "Initiate new onboarding or offboarding workflow",
            description = "Creates a new workflow instance from a template and assigns initial tasks. Only HR administrators can initiate workflows."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Workflow created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = WorkflowInitiationResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "workflowInstanceId": "550e8400-e29b-41d4-a716-446655440000",
                                              "employeeName": "John Doe",
                                              "status": "IN_PROGRESS",
                                              "totalTasksCreated": 15,
                                              "tasksAssigned": 3,
                                              "initiatedAt": "2025-10-31T10:30:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request - validation failure or template issue",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "timestamp": "2025-10-31T10:30:00",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Employee name is required",
                                              "path": "/api/workflows"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Only HR_ADMIN role can initiate workflows",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "timestamp": "2025-10-31T10:30:00",
                                              "status": 403,
                                              "error": "Forbidden",
                                              "message": "Access Denied",
                                              "path": "/api/workflows"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Template not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "timestamp": "2025-10-31T10:30:00",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "Workflow template with ID 550e8400-e29b-41d4-a716-446655440000 not found",
                                              "path": "/api/workflows"
                                            }
                                            """
                            )
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Workflow initiation request",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = InitiateWorkflowRequest.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "templateId": "550e8400-e29b-41d4-a716-446655440000",
                                      "employeeName": "John Doe",
                                      "employeeEmail": "john.doe@company.com",
                                      "employeeRole": "Software Engineer",
                                      "customFieldValues": {
                                        "startDate": "2025-02-01",
                                        "remoteStatus": "hybrid"
                                      }
                                    }
                                    """
                    )
            )
    )
    @PreAuthorize("hasRole('HR_ADMIN')")
    @PostMapping
    public ResponseEntity<WorkflowInitiationResponse> initiateWorkflow(
            @Valid @RequestBody InitiateWorkflowRequest request) {

        log.info("Initiating workflow for employee: {} using template: {}",
                request.getEmployeeName(), request.getTemplateId());

        UUID currentUserId = getCurrentUserId();

        // Create employee details object for service call
        EmployeeDetails employeeDetails = new EmployeeDetails();
        employeeDetails.setEmployeeName(request.getEmployeeName());
        employeeDetails.setEmployeeEmail(request.getEmployeeEmail());
        employeeDetails.setEmployeeRole(request.getEmployeeRole());

        // Create workflow instance
        WorkflowCreationResult creationResult = workflowService.createWorkflowInstance(
                request.getTemplateId(),
                employeeDetails,
                request.getCustomFieldValues(),
                currentUserId
        );

        log.debug("Workflow instance created with ID: {}, total tasks: {}",
                creationResult.getWorkflowInstanceId(), creationResult.getTotalTasks());

        // Assign initial tasks
        List<TaskAssignmentResult> assignmentResults = workflowService.assignTasksForWorkflow(
                creationResult.getWorkflowInstanceId()
        );

        log.info("Workflow {} initiated successfully for employee: {} by user: {}. {} tasks assigned.",
                creationResult.getWorkflowInstanceId(), request.getEmployeeName(),
                currentUserId, assignmentResults.size());

        // Build response
        WorkflowInitiationResponse response = new WorkflowInitiationResponse();
        response.setWorkflowInstanceId(creationResult.getWorkflowInstanceId());
        response.setEmployeeName(request.getEmployeeName());
        response.setStatus(WorkflowStatus.IN_PROGRESS); // After task assignment, workflow is IN_PROGRESS
        response.setTotalTasksCreated(creationResult.getTotalTasks());
        response.setTasksAssigned(assignmentResults.size());
        response.setInitiatedAt(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Gets the current authenticated user's ID from Spring Security context.
     *
     * @return UUID of the current user
     * @throws UnauthorizedException if no authenticated user found
     */
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("No authenticated user found in security context");
            throw new UnauthorizedException("User must be authenticated to initiate workflows");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User)) {
            log.error("Authentication principal is not a User object: {}", principal.getClass().getName());
            throw new UnauthorizedException("Invalid authentication principal");
        }

        User user = (User) principal;
        return user.getId();
    }
}
