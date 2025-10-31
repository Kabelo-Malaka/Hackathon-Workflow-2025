package com.magnab.employeelifecycle.service;

import com.magnab.employeelifecycle.dto.request.EmployeeDetails;
import com.magnab.employeelifecycle.dto.response.WorkflowCreationResult;
import com.magnab.employeelifecycle.entity.*;
import com.magnab.employeelifecycle.enums.TaskStatus;
import com.magnab.employeelifecycle.enums.WorkflowStatus;
import com.magnab.employeelifecycle.exception.ResourceNotFoundException;
import com.magnab.employeelifecycle.exception.ValidationException;
import com.magnab.employeelifecycle.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

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
}
