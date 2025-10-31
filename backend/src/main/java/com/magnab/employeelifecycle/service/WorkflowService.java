package com.magnab.employeelifecycle.service;

import com.magnab.employeelifecycle.dto.request.EmployeeDetails;
import com.magnab.employeelifecycle.dto.response.TaskAssignmentResult;
import com.magnab.employeelifecycle.dto.response.WorkflowCreationResult;
import com.magnab.employeelifecycle.entity.*;
import com.magnab.employeelifecycle.enums.TaskStatus;
import com.magnab.employeelifecycle.enums.UserRole;
import com.magnab.employeelifecycle.enums.WorkflowStatus;
import com.magnab.employeelifecycle.exception.ResourceNotFoundException;
import com.magnab.employeelifecycle.exception.ValidationException;
import com.magnab.employeelifecycle.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing workflow instances and their lifecycle.
 * Handles workflow instantiation, task creation, and conditional logic evaluation.
 */
@Service
@Slf4j
public class WorkflowService {

    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final TaskInstanceRepository taskInstanceRepository;
    private final WorkflowStateHistoryRepository workflowStateHistoryRepository;
    private final WorkflowTemplateRepository workflowTemplateRepository;
    private final TemplateTaskRepository templateTaskRepository;
    private final UserRepository userRepository;

    public WorkflowService(
            WorkflowInstanceRepository workflowInstanceRepository,
            TaskInstanceRepository taskInstanceRepository,
            WorkflowStateHistoryRepository workflowStateHistoryRepository,
            WorkflowTemplateRepository workflowTemplateRepository,
            TemplateTaskRepository templateTaskRepository,
            UserRepository userRepository
    ) {
        this.workflowInstanceRepository = workflowInstanceRepository;
        this.taskInstanceRepository = taskInstanceRepository;
        this.workflowStateHistoryRepository = workflowStateHistoryRepository;
        this.workflowTemplateRepository = workflowTemplateRepository;
        this.templateTaskRepository = templateTaskRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a new workflow instance from a template.
     * MVP scope: Creates workflow with all tasks visible. Conditional visibility deferred to future story.
     *
     * @param templateId The ID of the workflow template to instantiate
     * @param employeeDetails Employee information for the workflow
     * @param customFieldValues Custom field values for workflow instance
     * @param initiatedBy The user ID initiating the workflow
     * @return WorkflowCreationResult containing instance ID and task counts
     * @throws ResourceNotFoundException if template or user not found
     * @throws ValidationException if employee details are invalid or template is not active
     */
    @Transactional
    public WorkflowCreationResult createWorkflowInstance(
            UUID templateId,
            EmployeeDetails employeeDetails,
            Map<String, Object> customFieldValues,
            UUID initiatedBy
    ) {
        // Validate employee details first (before logging)
        validateEmployeeDetails(employeeDetails);

        log.info("Creating workflow instance from template: {} for employee: {}",
                templateId, employeeDetails.getEmployeeName());

        // Validate template exists and is active
        WorkflowTemplate template = workflowTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Workflow template not found with id: " + templateId));

        if (!template.getIsActive()) {
            throw new ValidationException("Cannot instantiate inactive workflow template: " + templateId);
        }

        // Validate initiating user exists
        User initiatingUser = userRepository.findById(initiatedBy)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + initiatedBy));

        // Create workflow instance with INITIATED status
        WorkflowInstance workflowInstance = createWorkflowInstanceEntity(
                template, employeeDetails, customFieldValues, initiatingUser);
        workflowInstance = workflowInstanceRepository.save(workflowInstance);
        log.debug("Created workflow instance: {}", workflowInstance.getId());

        // Fetch all template tasks and create task instances
        List<TemplateTask> templateTasks = templateTaskRepository.findByTemplateIdOrderBySequenceOrder(templateId);
        List<TaskInstance> taskInstances = createTaskInstances(
                workflowInstance, templateTasks, customFieldValues);
        taskInstanceRepository.saveAll(taskInstances);
        log.debug("Created {} task instances", taskInstances.size());

        // Create initial workflow state history record
        createInitialStateHistory(workflowInstance, initiatingUser);

        // Calculate summary statistics
        WorkflowCreationResult result = calculateWorkflowSummary(workflowInstance, taskInstances);

        log.info("Workflow instance {} created successfully with {} total tasks, {} immediately visible",
                result.getWorkflowInstanceId(), result.getTotalTasks(), result.getImmediateTasksCount());

        return result;
    }

    /**
     * Validates employee details are complete and properly formatted.
     */
    private void validateEmployeeDetails(EmployeeDetails employeeDetails) {
        if (employeeDetails == null) {
            throw new ValidationException("Employee details are required");
        }
        // Jakarta Bean Validation annotations will handle field-level validation
    }

    /**
     * Creates a WorkflowInstance entity from template and employee details.
     */
    private WorkflowInstance createWorkflowInstanceEntity(
            WorkflowTemplate template,
            EmployeeDetails employeeDetails,
            Map<String, Object> customFieldValues,
            User initiatingUser
    ) {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setTemplateId(template.getId());
        instance.setEmployeeName(employeeDetails.getEmployeeName());
        instance.setEmployeeEmail(employeeDetails.getEmployeeEmail());
        instance.setEmployeeRole(employeeDetails.getEmployeeRole());
        instance.setWorkflowType(template.getWorkflowType());
        instance.setStatus(WorkflowStatus.INITIATED);
        instance.setInitiatedBy(initiatingUser.getId());
        instance.setInitiatedAt(LocalDateTime.now());
        instance.setCustomFieldValues(customFieldValues != null ? customFieldValues : new HashMap<>());
        return instance;
    }

    /**
     * Creates task instances from template tasks.
     * MVP: All tasks are visible by default. Conditional visibility will be added in future story.
     */
    private List<TaskInstance> createTaskInstances(
            WorkflowInstance workflowInstance,
            List<TemplateTask> templateTasks,
            Map<String, Object> customFieldValues
    ) {
        List<TaskInstance> taskInstances = new ArrayList<>();

        for (TemplateTask templateTask : templateTasks) {
            TaskInstance taskInstance = new TaskInstance();
            taskInstance.setWorkflowInstanceId(workflowInstance.getId());
            taskInstance.setTemplateTaskId(templateTask.getId());
            taskInstance.setTaskName(templateTask.getTaskName());
            taskInstance.setSequenceOrder(templateTask.getSequenceOrder());
            taskInstance.setAssignedRole(templateTask.getAssignedRole());
            taskInstance.setStatus(TaskStatus.NOT_STARTED);

            // MVP: All tasks visible by default
            // TODO: Story 3.3 will add conditional_rules field to template_tasks and implement conditional visibility
            taskInstance.setIsVisible(true);

            taskInstances.add(taskInstance);
        }

        return taskInstances;
    }

    /**
     * Creates the initial workflow state history record.
     */
    private void createInitialStateHistory(WorkflowInstance workflowInstance, User initiatingUser) {
        WorkflowStateHistory history = new WorkflowStateHistory();
        history.setWorkflowInstanceId(workflowInstance.getId());
        history.setPreviousStatus(WorkflowStatus.INITIATED); // Initial state uses same status for both
        history.setNewStatus(WorkflowStatus.INITIATED);
        history.setChangedBy(initiatingUser.getId());
        history.setChangedAt(LocalDateTime.now());
        history.setNotes("Workflow initiated");
        workflowStateHistoryRepository.save(history);
        log.debug("Created initial state history for workflow: {}", workflowInstance.getId());
    }

    /**
     * Calculates workflow creation summary statistics.
     */
    private WorkflowCreationResult calculateWorkflowSummary(
            WorkflowInstance workflowInstance,
            List<TaskInstance> taskInstances
    ) {
        WorkflowCreationResult result = new WorkflowCreationResult();
        result.setWorkflowInstanceId(workflowInstance.getId());
        result.setTotalTasks(taskInstances.size());

        long immediateTasksCount = taskInstances.stream()
                .filter(TaskInstance::getIsVisible)
                .count();
        result.setImmediateTasksCount((int) immediateTasksCount);

        return result;
    }

    /**
     * Assigns tasks for a workflow instance to appropriate users based on role and workload.
     * Implements automatic task routing with load balancing and dependency checking.
     *
     * Algorithm:
     * 1. Filter tasks that are ready to assign (NOT_STARTED, visible, dependencies satisfied)
     * 2. For each task, find active users with matching role
     * 3. Select user with fewest IN_PROGRESS tasks (load balancing)
     * 4. Assign task to user and set status to IN_PROGRESS
     * 5. Set due date to 2 days from now (MVP scope)
     * 6. Update workflow status to IN_PROGRESS if this is the first assignment
     *
     * Idempotent: Can be called multiple times without error. Already-assigned tasks are skipped.
     *
     * @param workflowInstanceId The ID of the workflow instance to assign tasks for
     * @return List of TaskAssignmentResult for each newly assigned task
     * @throws ResourceNotFoundException if workflow instance not found
     */
    @Transactional
    public List<TaskAssignmentResult> assignTasksForWorkflow(UUID workflowInstanceId) {
        log.info("Assigning tasks for workflow instance: {}", workflowInstanceId);

        // Validate workflow instance exists
        WorkflowInstance workflowInstance = workflowInstanceRepository.findById(workflowInstanceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Workflow instance not found with id: " + workflowInstanceId));

        // Get all task instances ordered by sequence
        List<TaskInstance> allTasks = taskInstanceRepository
                .findByWorkflowInstanceIdOrderBySequenceOrder(workflowInstanceId);

        // Get template tasks for dependency checking
        Map<UUID, TemplateTask> templateTaskMap = templateTaskRepository
                .findByTemplateIdOrderBySequenceOrder(workflowInstance.getTemplateId())
                .stream()
                .collect(Collectors.toMap(TemplateTask::getId, tt -> tt));

        // Filter tasks ready to assign (idempotency: skip already assigned)
        List<TaskInstance> readyTasks = filterReadyToAssignTasks(allTasks, templateTaskMap);
        log.debug("Found {} tasks ready to assign", readyTasks.size());

        // Track if this is the first assignment for workflow status update
        boolean isFirstAssignment = allTasks.stream()
                .noneMatch(task -> task.getAssignedUserId() != null);

        // Assign each ready task
        List<TaskAssignmentResult> results = new ArrayList<>();
        for (TaskInstance task : readyTasks) {
            TaskAssignmentResult result = assignTaskToUser(task, templateTaskMap);
            if (result != null) {
                results.add(result);
            }
        }

        // Save all assigned tasks
        if (!results.isEmpty()) {
            taskInstanceRepository.saveAll(readyTasks);
            log.debug("Saved {} assigned tasks", results.size());
        }

        // Update workflow status to IN_PROGRESS if first assignment
        if (isFirstAssignment && !results.isEmpty()) {
            updateWorkflowStatusToInProgress(workflowInstance);
        }

        log.info("Assigned {} tasks for workflow instance: {}", results.size(), workflowInstanceId);
        return results;
    }

    /**
     * Filters tasks that are ready to be assigned.
     * Ready criteria: NOT_STARTED status, visible, and all dependencies satisfied.
     * Implements idempotency by skipping already-assigned tasks.
     */
    private List<TaskInstance> filterReadyToAssignTasks(
            List<TaskInstance> allTasks,
            Map<UUID, TemplateTask> templateTaskMap
    ) {
        return allTasks.stream()
                .filter(task -> {
                    // Idempotency: skip already assigned tasks
                    if (task.getAssignedUserId() != null) {
                        return false;
                    }

                    // Must be NOT_STARTED
                    if (task.getStatus() != TaskStatus.NOT_STARTED) {
                        return false;
                    }

                    // Must be visible
                    if (!task.getIsVisible()) {
                        return false;
                    }

                    // Check dependencies satisfied
                    return isDependencySatisfied(task, allTasks, templateTaskMap);
                })
                .collect(Collectors.toList());
    }

    /**
     * Checks if a task's dependencies are satisfied.
     * A task is ready if:
     * - It has no dependency (depends_on_task_id is null), OR
     * - Its dependent task has status COMPLETED
     */
    private boolean isDependencySatisfied(
            TaskInstance task,
            List<TaskInstance> allTasks,
            Map<UUID, TemplateTask> templateTaskMap
    ) {
        TemplateTask templateTask = templateTaskMap.get(task.getTemplateTaskId());
        if (templateTask == null) {
            log.warn("Template task not found for task instance: {}", task.getId());
            return false;
        }

        // No dependency = ready
        if (templateTask.getDependsOnTask() == null) {
            return true;
        }

        UUID dependencyTemplateTaskId = templateTask.getDependsOnTask().getId();

        // Find the task instance for this dependency
        Optional<TaskInstance> dependentTaskOpt = allTasks.stream()
                .filter(t -> t.getTemplateTaskId().equals(dependencyTemplateTaskId))
                .findFirst();

        if (!dependentTaskOpt.isPresent()) {
            log.warn("Dependent task instance not found for template task: {}", dependencyTemplateTaskId);
            return false;
        }

        TaskInstance dependentTask = dependentTaskOpt.get();
        return dependentTask.getStatus() == TaskStatus.COMPLETED;
    }

    /**
     * Assigns a task to the best available user based on role and load balancing.
     * Returns TaskAssignmentResult if assignment successful, null otherwise.
     */
    private TaskAssignmentResult assignTaskToUser(
            TaskInstance task,
            Map<UUID, TemplateTask> templateTaskMap
    ) {
        TemplateTask templateTask = templateTaskMap.get(task.getTemplateTaskId());
        if (templateTask == null) {
            log.error("Template task not found for task instance: {}", task.getId());
            return null;
        }

        UserRole requiredRole = templateTask.getAssignedRole();

        // Find all active users with the required role
        List<User> eligibleUsers = userRepository.findByRoleAndIsActive(requiredRole, true);
        if (eligibleUsers.isEmpty()) {
            log.warn("No active users found for role: {}", requiredRole);
            return null;
        }

        // Load balancing: select user with fewest IN_PROGRESS tasks
        User selectedUser = selectUserWithLeastLoad(eligibleUsers);

        // Assign task to selected user
        task.setAssignedUserId(selectedUser.getId());
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setDueDate(LocalDateTime.now().plusDays(2)); // MVP: 2-day SLA

        log.debug("Assigned task {} to user {} ({})", task.getTaskName(),
                selectedUser.getEmail(), selectedUser.getRole());

        // Build result DTO
        TaskAssignmentResult result = new TaskAssignmentResult();
        result.setTaskInstanceId(task.getId());
        result.setAssignedUserId(selectedUser.getId());
        result.setAssignedUserEmail(selectedUser.getEmail());
        result.setTaskName(task.getTaskName());
        result.setDueDate(task.getDueDate());

        return result;
    }

    /**
     * Selects the user with the least workload (fewest IN_PROGRESS tasks).
     * Implements round-robin load balancing for fair task distribution.
     */
    private User selectUserWithLeastLoad(List<User> eligibleUsers) {
        User selectedUser = eligibleUsers.get(0);
        long minLoad = taskInstanceRepository.countByAssignedUserIdAndStatus(
                selectedUser.getId(), TaskStatus.IN_PROGRESS);

        for (int i = 1; i < eligibleUsers.size(); i++) {
            User user = eligibleUsers.get(i);
            long userLoad = taskInstanceRepository.countByAssignedUserIdAndStatus(
                    user.getId(), TaskStatus.IN_PROGRESS);

            if (userLoad < minLoad) {
                minLoad = userLoad;
                selectedUser = user;
            }
        }

        log.debug("Selected user {} with {} IN_PROGRESS tasks", selectedUser.getEmail(), minLoad);
        return selectedUser;
    }

    /**
     * Updates workflow status to IN_PROGRESS when first task is assigned.
     * Creates workflow state history record for audit trail.
     */
    private void updateWorkflowStatusToInProgress(WorkflowInstance workflowInstance) {
        WorkflowStatus previousStatus = workflowInstance.getStatus();
        workflowInstance.setStatus(WorkflowStatus.IN_PROGRESS);
        workflowInstanceRepository.save(workflowInstance);

        // Create state history record
        WorkflowStateHistory history = new WorkflowStateHistory();
        history.setWorkflowInstanceId(workflowInstance.getId());
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(WorkflowStatus.IN_PROGRESS);
        history.setChangedBy(workflowInstance.getInitiatedBy()); // System uses initiator for auto-assignment
        history.setChangedAt(LocalDateTime.now());
        history.setNotes("Workflow status updated to IN_PROGRESS after first task assignment");
        workflowStateHistoryRepository.save(history);

        log.info("Updated workflow {} status from {} to IN_PROGRESS",
                workflowInstance.getId(), previousStatus);
    }
}
