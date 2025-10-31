package com.magnab.employeelifecycle.controller;

import com.magnab.employeelifecycle.dto.request.EmployeeDetails;
import com.magnab.employeelifecycle.dto.request.InitiateWorkflowRequest;
import com.magnab.employeelifecycle.dto.response.*;
import com.magnab.employeelifecycle.entity.User;
import com.magnab.employeelifecycle.enums.UserRole;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

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
     * Retrieves a paginated list of workflows with optional filtering and sorting.
     * HR_ADMIN users see all workflows; other roles see only workflows where they have assigned tasks.
     *
     * @param status Optional workflow status filter
     * @param workflowType Optional workflow type filter
     * @param employeeName Optional employee name search (case-insensitive partial match)
     * @param sortBy Field to sort by (default: initiatedAt)
     * @param sortDirection Sort direction: asc or desc (default: desc)
     * @param page Page number (0-indexed, default: 0)
     * @param size Page size (default: 50)
     * @return Page of WorkflowSummaryResponse objects
     */
    @Operation(
            summary = "Get paginated list of workflows",
            description = "Retrieves workflows with optional filtering by status, type, and employee name. " +
                    "Supports pagination and sorting. HR_ADMIN users see all workflows; " +
                    "other roles see only workflows where they have assigned tasks."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved workflows",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Page.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "content": [
                                                {
                                                  "id": "550e8400-e29b-41d4-a716-446655440000",
                                                  "employeeName": "John Doe",
                                                  "workflowType": "ONBOARDING",
                                                  "status": "IN_PROGRESS",
                                                  "initiatedAt": "2025-10-30T10:30:00",
                                                  "totalTasks": 15,
                                                  "completedTasks": 5
                                                },
                                                {
                                                  "id": "550e8400-e29b-41d4-a716-446655440001",
                                                  "employeeName": "Jane Smith",
                                                  "workflowType": "OFFBOARDING",
                                                  "status": "COMPLETED",
                                                  "initiatedAt": "2025-10-25T14:20:00",
                                                  "totalTasks": 12,
                                                  "completedTasks": 12
                                                }
                                              ],
                                              "pageable": {
                                                "pageNumber": 0,
                                                "pageSize": 50,
                                                "sort": {
                                                  "sorted": true,
                                                  "orders": [{"property": "initiatedAt", "direction": "DESC"}]
                                                }
                                              },
                                              "totalElements": 2,
                                              "totalPages": 1,
                                              "last": true,
                                              "first": true,
                                              "size": 50,
                                              "number": 0
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid filter parameters",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "timestamp": "2025-10-31T10:30:00",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Invalid workflow status: INVALID_STATUS",
                                              "path": "/api/workflows"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "timestamp": "2025-10-31T10:30:00",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "User must be authenticated to access workflows",
                                              "path": "/api/workflows"
                                            }
                                            """
                            )
                    )
            )
    })
    @io.swagger.v3.oas.annotations.Parameter(
            name = "status",
            description = "Filter by workflow status (IN_PROGRESS, COMPLETED, CANCELLED)",
            example = "IN_PROGRESS"
    )
    @io.swagger.v3.oas.annotations.Parameter(
            name = "workflowType",
            description = "Filter by workflow type (ONBOARDING, OFFBOARDING)",
            example = "ONBOARDING"
    )
    @io.swagger.v3.oas.annotations.Parameter(
            name = "employeeName",
            description = "Search by employee name (case-insensitive partial match)",
            example = "John"
    )
    @io.swagger.v3.oas.annotations.Parameter(
            name = "sortBy",
            description = "Field to sort by",
            example = "initiatedAt"
    )
    @io.swagger.v3.oas.annotations.Parameter(
            name = "sortDirection",
            description = "Sort direction (asc or desc)",
            example = "desc"
    )
    @io.swagger.v3.oas.annotations.Parameter(
            name = "page",
            description = "Page number (0-indexed)",
            example = "0"
    )
    @io.swagger.v3.oas.annotations.Parameter(
            name = "size",
            description = "Page size (number of workflows per page)",
            example = "50"
    )
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<Page<WorkflowSummaryResponse>> getWorkflows(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String workflowType,
            @RequestParam(required = false) String employeeName,
            @RequestParam(defaultValue = "initiatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        log.info("GET /api/workflows - Fetching workflows with filters: status={}, type={}, name={}, page={}, size={}",
                status, workflowType, employeeName, page, size);

        UUID currentUserId = getCurrentUserId();
        UserRole currentUserRole = getCurrentUserRole();

        Page<WorkflowSummaryResponse> workflows = workflowService.getWorkflows(
                status,
                workflowType,
                employeeName,
                sortBy,
                sortDirection,
                page,
                size,
                currentUserId,
                currentUserRole
        );

        log.debug("Retrieved {} workflows for user: {}", workflows.getTotalElements(), currentUserId);

        return ResponseEntity.ok(workflows);
    }

    /**
     * Retrieves detailed information about a specific workflow.
     * HR_ADMIN users can view any workflow; other roles can only view workflows where they have assigned tasks.
     *
     * @param id Workflow instance ID
     * @return WorkflowDetailResponse with complete workflow information
     * @throws ResourceNotFoundException if workflow not found
     * @throws ForbiddenException if user is not authorized to view the workflow
     */
    @Operation(
            summary = "Get detailed workflow information by ID",
            description = "Retrieves complete workflow details including metadata, custom fields, all task instances, " +
                    "and state history. HR_ADMIN users can view any workflow; other roles can only view workflows " +
                    "where they have assigned tasks."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved workflow details",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = WorkflowDetailResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "id": "550e8400-e29b-41d4-a716-446655440000",
                                              "employeeName": "John Doe",
                                              "employeeEmail": "john.doe@company.com",
                                              "employeeRole": "Software Engineer",
                                              "workflowType": "ONBOARDING",
                                              "status": "IN_PROGRESS",
                                              "initiatedAt": "2025-10-30T10:30:00",
                                              "completedAt": null,
                                              "initiatedBy": "550e8400-e29b-41d4-a716-446655440010",
                                              "customFieldValues": {
                                                "startDate": "2025-02-01",
                                                "remoteStatus": "hybrid"
                                              },
                                              "tasks": [
                                                {
                                                  "id": "550e8400-e29b-41d4-a716-446655440020",
                                                  "taskName": "Send welcome email",
                                                  "status": "COMPLETED",
                                                  "assignedUserId": "550e8400-e29b-41d4-a716-446655440030",
                                                  "assignedUserName": "Alice HR",
                                                  "assignedRole": "HR_ADMIN",
                                                  "isVisible": true,
                                                  "dueDate": "2025-10-31T17:00:00",
                                                  "completedAt": "2025-10-30T11:15:00",
                                                  "completedBy": "550e8400-e29b-41d4-a716-446655440030"
                                                },
                                                {
                                                  "id": "550e8400-e29b-41d4-a716-446655440021",
                                                  "taskName": "Provision laptop",
                                                  "status": "IN_PROGRESS",
                                                  "assignedUserId": "550e8400-e29b-41d4-a716-446655440031",
                                                  "assignedUserName": "Bob IT",
                                                  "assignedRole": "IT_SUPPORT",
                                                  "isVisible": true,
                                                  "dueDate": "2025-11-01T17:00:00",
                                                  "completedAt": null,
                                                  "completedBy": null
                                                }
                                              ],
                                              "stateHistory": [
                                                {
                                                  "id": "550e8400-e29b-41d4-a716-446655440040",
                                                  "previousStatus": "PENDING",
                                                  "newStatus": "IN_PROGRESS",
                                                  "changedBy": "550e8400-e29b-41d4-a716-446655440030",
                                                  "changedByName": "Alice HR",
                                                  "changedAt": "2025-10-30T10:35:00",
                                                  "notes": "Initial task assignment completed"
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "timestamp": "2025-10-31T10:30:00",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "User must be authenticated to access workflows",
                                              "path": "/api/workflows/550e8400-e29b-41d4-a716-446655440000"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User is not authorized to view this workflow",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "timestamp": "2025-10-31T10:30:00",
                                              "status": 403,
                                              "error": "Forbidden",
                                              "message": "Access denied: You are not authorized to view this workflow",
                                              "path": "/api/workflows/550e8400-e29b-41d4-a716-446655440000"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Workflow not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "timestamp": "2025-10-31T10:30:00",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "Workflow not found with id: 550e8400-e29b-41d4-a716-446655440000",
                                              "path": "/api/workflows/550e8400-e29b-41d4-a716-446655440000"
                                            }
                                            """
                            )
                    )
            )
    })
    @io.swagger.v3.oas.annotations.Parameter(
            name = "id",
            description = "Workflow instance ID (UUID)",
            required = true,
            example = "550e8400-e29b-41d4-a716-446655440000"
    )
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<WorkflowDetailResponse> getWorkflowById(@PathVariable UUID id) {

        log.info("GET /api/workflows/{} - Fetching workflow details", id);

        UUID currentUserId = getCurrentUserId();
        UserRole currentUserRole = getCurrentUserRole();

        WorkflowDetailResponse workflow = workflowService.getWorkflowById(
                id,
                currentUserId,
                currentUserRole
        );

        log.debug("Retrieved workflow details for ID: {}, user: {}", id, currentUserId);

        return ResponseEntity.ok(workflow);
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

    /**
     * Gets the current authenticated user's role from Spring Security context.
     *
     * @return UserRole of the current user
     * @throws UnauthorizedException if no authenticated user found
     */
    private UserRole getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("No authenticated user found in security context");
            throw new UnauthorizedException("User must be authenticated to access workflows");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User)) {
            log.error("Authentication principal is not a User object: {}", principal.getClass().getName());
            throw new UnauthorizedException("Invalid authentication principal");
        }

        User user = (User) principal;
        return user.getRole();
    }
}
