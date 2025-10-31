package com.magnab.employeelifecycle.service;

import com.magnab.employeelifecycle.dto.request.CreateTemplateRequest;
import com.magnab.employeelifecycle.dto.request.CreateTemplateTaskRequest;
import com.magnab.employeelifecycle.dto.request.UpdateTemplateRequest;
import com.magnab.employeelifecycle.dto.response.TaskDetailResponse;
import com.magnab.employeelifecycle.dto.response.TemplateDetailResponse;
import com.magnab.employeelifecycle.dto.response.TemplateSummaryResponse;
import com.magnab.employeelifecycle.entity.TemplateTask;
import com.magnab.employeelifecycle.entity.User;
import com.magnab.employeelifecycle.entity.WorkflowTemplate;
import com.magnab.employeelifecycle.exception.ResourceNotFoundException;
import com.magnab.employeelifecycle.repository.TemplateTaskRepository;
import com.magnab.employeelifecycle.repository.UserRepository;
import com.magnab.employeelifecycle.repository.WorkflowTemplateRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service layer for workflow template management.
 * Handles business logic for CRUD operations on templates and tasks.
 */
@Service
@Transactional
public class TemplateService {

    private final WorkflowTemplateRepository templateRepository;
    private final TemplateTaskRepository taskRepository;
    private final UserRepository userRepository;

    public TemplateService(WorkflowTemplateRepository templateRepository,
                           TemplateTaskRepository taskRepository,
                           UserRepository userRepository) {
        this.templateRepository = templateRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create a new workflow template with tasks.
     *
     * @param request Request containing template and task data
     * @return Created template with all details
     */
    public TemplateDetailResponse createTemplate(CreateTemplateRequest request) {
        User currentUser = getCurrentUser();
        UUID userId = currentUser.getId();

        WorkflowTemplate template = new WorkflowTemplate();
        template.setTemplateName(request.getName());
        template.setDescription(request.getDescription());
        template.setWorkflowType(request.getType());
        template.setIsActive(true);
        template.setCreatedBy(userId);
        template.setUpdatedBy(userId);

        // Convert task DTOs to entities
        for (CreateTemplateTaskRequest taskRequest : request.getTasks()) {
            TemplateTask task = toTaskEntity(taskRequest);
            task.setTemplate(template);
            task.setCreatedBy(userId);
            task.setUpdatedBy(userId);
            template.getTasks().add(task);
        }

        WorkflowTemplate saved = templateRepository.save(template);
        return toDetailResponse(saved);
    }

    /**
     * Get all workflow templates with summary information.
     *
     * @return List of template summaries
     */
    @Transactional(readOnly = true)
    public List<TemplateSummaryResponse> getAllTemplates() {
        List<WorkflowTemplate> templates = templateRepository.findAll();
        return templates.stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a single template by ID with complete details.
     *
     * @param id Template ID
     * @return Complete template details including tasks
     * @throws ResourceNotFoundException if template not found
     */
    @Transactional(readOnly = true)
    public TemplateDetailResponse getTemplateById(UUID id) {
        WorkflowTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + id));
        return toDetailResponse(template);
    }

    /**
     * Update an existing workflow template.
     * Replaces the entire template structure (cascade update to tasks).
     *
     * @param id      Template ID
     * @param request Request containing updated template and task data
     * @return Updated template with all details
     * @throws ResourceNotFoundException if template not found
     */
    public TemplateDetailResponse updateTemplate(UUID id, UpdateTemplateRequest request) {
        User currentUser = getCurrentUser();
        UUID userId = currentUser.getId();

        WorkflowTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + id));

        // Update template fields
        template.setTemplateName(request.getName());
        template.setDescription(request.getDescription());
        template.setWorkflowType(request.getType());
        template.setIsActive(request.getIsActive());
        template.setUpdatedBy(userId);
        template.setUpdatedAt(LocalDateTime.now());

        // Replace tasks (clear old, add new)
        // orphanRemoval = true ensures old tasks are deleted
        template.getTasks().clear();

        for (CreateTemplateTaskRequest taskRequest : request.getTasks()) {
            TemplateTask task = toTaskEntity(taskRequest);
            task.setTemplate(template);
            task.setCreatedBy(userId);
            task.setUpdatedBy(userId);
            template.getTasks().add(task);
        }

        WorkflowTemplate updated = templateRepository.save(template);
        return toDetailResponse(updated);
    }

    /**
     * Delete a workflow template (soft delete: sets is_active=false).
     * Prevents deletion if template is in use by active workflows.
     *
     * @param id Template ID
     * @throws ResourceNotFoundException if template not found
     */
    public void deleteTemplate(UUID id) {
        User currentUser = getCurrentUser();
        UUID userId = currentUser.getId();

        WorkflowTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + id));

        // TODO: Check if template is in use by active workflows when WorkflowInstance is implemented in Epic 3
        // long activeWorkflowCount = workflowInstanceRepository.countByTemplateIdAndStatusNot(id, WorkflowStatus.COMPLETED);
        // if (activeWorkflowCount > 0) {
        //     throw new ConflictException("Template cannot be deleted as it is in use by active workflows");
        // }

        // Soft delete
        template.setIsActive(false);
        template.setUpdatedBy(userId);
        template.setUpdatedAt(LocalDateTime.now());
        templateRepository.save(template);
    }

    // === Private Helper Methods ===

    private TemplateTask toTaskEntity(CreateTemplateTaskRequest request) {
        TemplateTask task = new TemplateTask();
        task.setTaskName(request.getTaskName());
        task.setDescription(request.getDescription());
        task.setAssignedRole(request.getAssignedRole());
        task.setSequenceOrder(request.getSequenceOrder());
        task.setIsParallel(request.getIsParallel() != null ? request.getIsParallel() : false);

        // Handle dependency - will be null for new templates, actual UUID for updates
        if (request.getDependencyTaskId() != null) {
            TemplateTask dependency = new TemplateTask();
            dependency.setId(request.getDependencyTaskId());
            task.setDependsOnTask(dependency);
        }

        return task;
    }

    private TemplateDetailResponse toDetailResponse(WorkflowTemplate template) {
        TemplateDetailResponse response = new TemplateDetailResponse();
        response.setId(template.getId());
        response.setName(template.getTemplateName());
        response.setDescription(template.getDescription());
        response.setType(template.getWorkflowType());
        response.setIsActive(template.getIsActive());
        response.setCreatedAt(template.getCreatedAt());
        response.setCreatedBy(template.getCreatedBy());
        response.setUpdatedAt(template.getUpdatedAt());
        response.setUpdatedBy(template.getUpdatedBy());

        List<TaskDetailResponse> tasks = template.getTasks().stream()
                .map(this::toTaskDetailResponse)
                .collect(Collectors.toList());
        response.setTasks(tasks);

        return response;
    }

    private TaskDetailResponse toTaskDetailResponse(TemplateTask task) {
        TaskDetailResponse response = new TaskDetailResponse();
        response.setId(task.getId());
        response.setTaskName(task.getTaskName());
        response.setDescription(task.getDescription());
        response.setAssignedRole(task.getAssignedRole());
        response.setSequenceOrder(task.getSequenceOrder());
        response.setIsParallel(task.getIsParallel());
        response.setDependencyTaskId(task.getDependsOnTask() != null ? task.getDependsOnTask().getId() : null);
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        return response;
    }

    private TemplateSummaryResponse toSummaryResponse(WorkflowTemplate template) {
        TemplateSummaryResponse response = new TemplateSummaryResponse();
        response.setId(template.getId());
        response.setName(template.getTemplateName());
        response.setType(template.getWorkflowType());
        response.setIsActive(template.getIsActive());
        response.setTaskCount(template.getTasks().size());
        response.setCreatedAt(template.getCreatedAt());
        response.setUpdatedAt(template.getUpdatedAt());
        return response;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        return userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Current authenticated user not found: " + currentUsername));
    }
}
