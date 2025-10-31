package com.magnab.employeelifecycle.service;

import com.magnab.employeelifecycle.dto.request.CreateTemplateRequest;
import com.magnab.employeelifecycle.dto.request.CreateTemplateTaskRequest;
import com.magnab.employeelifecycle.dto.request.UpdateTemplateRequest;
import com.magnab.employeelifecycle.dto.response.TemplateDetailResponse;
import com.magnab.employeelifecycle.dto.response.TemplateSummaryResponse;
import com.magnab.employeelifecycle.entity.TemplateTask;
import com.magnab.employeelifecycle.entity.User;
import com.magnab.employeelifecycle.entity.WorkflowTemplate;
import com.magnab.employeelifecycle.enums.UserRole;
import com.magnab.employeelifecycle.enums.WorkflowStatus;
import com.magnab.employeelifecycle.enums.WorkflowType;
import com.magnab.employeelifecycle.exception.ResourceNotFoundException;
import com.magnab.employeelifecycle.exception.ValidationException;
import com.magnab.employeelifecycle.repository.TemplateTaskRepository;
import com.magnab.employeelifecycle.repository.UserRepository;
import com.magnab.employeelifecycle.repository.WorkflowTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TemplateService.
 * Tests business logic with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    @Mock
    private WorkflowTemplateRepository templateRepository;

    @Mock
    private TemplateTaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TemplateService templateService;

    private UUID userId;
    private User currentUser;
    private WorkflowTemplate mockTemplate;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        currentUser = new User();
        currentUser.setId(userId);
        currentUser.setUsername("admin");
        currentUser.setRole(UserRole.ADMINISTRATOR);

        // Mock Security Context
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("admin");
        SecurityContextHolder.setContext(securityContext);
        lenient().when(userRepository.findByUsername("admin")).thenReturn(Optional.of(currentUser));

        mockTemplate = createMockTemplate();
    }

    // === CREATE TEMPLATE TESTS ===

    @Test
    void createTemplate_WithValidData_ReturnsTemplateDetailResponse() {
        // Arrange
        CreateTemplateRequest request = new CreateTemplateRequest();
        request.setName("Onboarding Template");
        request.setDescription("Standard onboarding process");
        request.setType(WorkflowType.ONBOARDING);
        request.setTasks(List.of(
            createTaskRequest("Setup Workstation", UserRole.TECH_SUPPORT, 1),
            createTaskRequest("HR Orientation", UserRole.HR_ADMIN, 2)
        ));

        WorkflowTemplate savedTemplate = createMockTemplate();
        savedTemplate.setId(UUID.randomUUID());
        when(templateRepository.save(any(WorkflowTemplate.class))).thenReturn(savedTemplate);

        // Act
        TemplateDetailResponse result = templateService.createTemplate(request);

        // Assert
        assertNotNull(result);
        assertEquals("Standard Onboarding", result.getName());
        assertEquals(WorkflowType.ONBOARDING, result.getType());
        assertTrue(result.getIsActive());
        assertEquals(2, result.getTasks().size());

        ArgumentCaptor<WorkflowTemplate> captor = ArgumentCaptor.forClass(WorkflowTemplate.class);
        verify(templateRepository).save(captor.capture());

        WorkflowTemplate captured = captor.getValue();
        assertEquals("Onboarding Template", captured.getTemplateName());
        assertEquals(userId, captured.getCreatedBy());
        assertEquals(userId, captured.getUpdatedBy());
        assertEquals(2, captured.getTasks().size());
    }

    @Test
    void createTemplate_SetsAuditFields() {
        // Arrange
        CreateTemplateRequest request = new CreateTemplateRequest();
        request.setName("Test Template");
        request.setType(WorkflowType.ONBOARDING);
        request.setTasks(List.of(createTaskRequest("Task 1", UserRole.HR_ADMIN, 1)));

        WorkflowTemplate savedTemplate = createMockTemplate();
        when(templateRepository.save(any(WorkflowTemplate.class))).thenReturn(savedTemplate);

        // Act
        templateService.createTemplate(request);

        // Assert
        ArgumentCaptor<WorkflowTemplate> captor = ArgumentCaptor.forClass(WorkflowTemplate.class);
        verify(templateRepository).save(captor.capture());

        WorkflowTemplate captured = captor.getValue();
        assertEquals(userId, captured.getCreatedBy());
        assertEquals(userId, captured.getUpdatedBy());
        assertTrue(captured.getIsActive());

        // Verify task audit fields
        for (TemplateTask task : captured.getTasks()) {
            assertEquals(userId, task.getCreatedBy());
            assertEquals(userId, task.getUpdatedBy());
            assertNotNull(task.getTemplate());
        }
    }

    // === GET ALL TEMPLATES TESTS ===

    @Test
    void getAllTemplates_ReturnsListOfSummaries() {
        // Arrange
        List<WorkflowTemplate> templates = Arrays.asList(
            createMockTemplate(),
            createMockTemplate(),
            createMockTemplate()
        );
        when(templateRepository.findAll()).thenReturn(templates);

        // Act
        List<TemplateSummaryResponse> result = templateService.getAllTemplates();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(templateRepository).findAll();
    }

    @Test
    void getAllTemplates_WithEmptyDatabase_ReturnsEmptyList() {
        // Arrange
        when(templateRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<TemplateSummaryResponse> result = templateService.getAllTemplates();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(templateRepository).findAll();
    }

    @Test
    void getAllTemplates_IncludesTaskCount() {
        // Arrange
        WorkflowTemplate template = createMockTemplate();
        template.getTasks().add(createMockTask("Additional Task", 3));
        when(templateRepository.findAll()).thenReturn(List.of(template));

        // Act
        List<TemplateSummaryResponse> result = templateService.getAllTemplates();

        // Assert
        assertEquals(1, result.size());
        assertEquals(3, result.get(0).getTaskCount());
    }

    // === GET TEMPLATE BY ID TESTS ===

    @Test
    void getTemplateById_WithExistingId_ReturnsTemplateDetail() {
        // Arrange
        UUID templateId = UUID.randomUUID();
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));

        // Act
        TemplateDetailResponse result = templateService.getTemplateById(templateId);

        // Assert
        assertNotNull(result);
        assertEquals("Standard Onboarding", result.getName());
        assertEquals(2, result.getTasks().size());
        verify(templateRepository).findById(templateId);
    }

    @Test
    void getTemplateById_WithNonExistentId_ThrowsResourceNotFoundException() {
        // Arrange
        UUID templateId = UUID.randomUUID();
        when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> templateService.getTemplateById(templateId)
        );

        assertTrue(exception.getMessage().contains("Template not found"));
        assertTrue(exception.getMessage().contains(templateId.toString()));
        verify(templateRepository).findById(templateId);
    }

    // === UPDATE TEMPLATE TESTS ===

    @Test
    void updateTemplate_WithValidData_ReturnsUpdatedTemplate() {
        // Arrange
        UUID templateId = UUID.randomUUID();
        mockTemplate.setId(templateId);
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));
        when(templateRepository.save(any(WorkflowTemplate.class))).thenReturn(mockTemplate);

        UpdateTemplateRequest request = new UpdateTemplateRequest();
        request.setName("Updated Template");
        request.setDescription("Updated description");
        request.setType(WorkflowType.OFFBOARDING);
        request.setIsActive(false);
        request.setTasks(List.of(
            createTaskRequest("New Task", UserRole.LINE_MANAGER, 1)
        ));

        // Act
        TemplateDetailResponse result = templateService.updateTemplate(templateId, request);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<WorkflowTemplate> captor = ArgumentCaptor.forClass(WorkflowTemplate.class);
        verify(templateRepository).save(captor.capture());

        WorkflowTemplate captured = captor.getValue();
        assertEquals("Updated Template", captured.getTemplateName());
        assertEquals("Updated description", captured.getDescription());
        assertEquals(WorkflowType.OFFBOARDING, captured.getWorkflowType());
        assertEquals(false, captured.getIsActive());
        assertEquals(userId, captured.getUpdatedBy());
        assertNotNull(captured.getUpdatedAt());
    }

    @Test
    void updateTemplate_ReplacesAllTasks() {
        // Arrange
        UUID templateId = UUID.randomUUID();
        mockTemplate.setId(templateId);

        // Mock template has 2 tasks initially
        assertEquals(2, mockTemplate.getTasks().size());

        when(templateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));
        when(templateRepository.save(any(WorkflowTemplate.class))).thenReturn(mockTemplate);

        UpdateTemplateRequest request = new UpdateTemplateRequest();
        request.setName("Updated Template");
        request.setType(WorkflowType.ONBOARDING);
        request.setIsActive(true);
        request.setTasks(List.of(
            createTaskRequest("Only Task", UserRole.HR_ADMIN, 1)
        ));

        // Act
        templateService.updateTemplate(templateId, request);

        // Assert
        ArgumentCaptor<WorkflowTemplate> captor = ArgumentCaptor.forClass(WorkflowTemplate.class);
        verify(templateRepository).save(captor.capture());

        WorkflowTemplate captured = captor.getValue();
        assertEquals(1, captured.getTasks().size());
        assertEquals("Only Task", captured.getTasks().get(0).getTaskName());
    }

    @Test
    void updateTemplate_WithNonExistentId_ThrowsResourceNotFoundException() {
        // Arrange
        UUID templateId = UUID.randomUUID();
        when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

        UpdateTemplateRequest request = new UpdateTemplateRequest();
        request.setName("Test");
        request.setType(WorkflowType.ONBOARDING);
        request.setIsActive(true);
        request.setTasks(List.of(createTaskRequest("Task", UserRole.HR_ADMIN, 1)));

        // Act & Assert
        assertThrows(
            ResourceNotFoundException.class,
            () -> templateService.updateTemplate(templateId, request)
        );

        verify(templateRepository).findById(templateId);
        verify(templateRepository, never()).save(any());
    }

    @Test
    void updateTemplate_UpdatesAuditFields() {
        // Arrange
        UUID templateId = UUID.randomUUID();
        mockTemplate.setId(templateId);
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));
        when(templateRepository.save(any(WorkflowTemplate.class))).thenReturn(mockTemplate);

        UpdateTemplateRequest request = new UpdateTemplateRequest();
        request.setName("Updated");
        request.setType(WorkflowType.ONBOARDING);
        request.setIsActive(true);
        request.setTasks(List.of(createTaskRequest("Task", UserRole.HR_ADMIN, 1)));

        LocalDateTime beforeUpdate = LocalDateTime.now();

        // Act
        templateService.updateTemplate(templateId, request);

        // Assert
        ArgumentCaptor<WorkflowTemplate> captor = ArgumentCaptor.forClass(WorkflowTemplate.class);
        verify(templateRepository).save(captor.capture());

        WorkflowTemplate captured = captor.getValue();
        assertEquals(userId, captured.getUpdatedBy());
        assertNotNull(captured.getUpdatedAt());
        assertTrue(captured.getUpdatedAt().isAfter(beforeUpdate) ||
                   captured.getUpdatedAt().isEqual(beforeUpdate));
    }

    // === DELETE TEMPLATE TESTS ===

    @Test
    void deleteTemplate_WithExistingId_SoftDeletesTemplate() {
        // Arrange
        UUID templateId = UUID.randomUUID();
        mockTemplate.setId(templateId);
        mockTemplate.setIsActive(true);
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));
        when(templateRepository.save(any(WorkflowTemplate.class))).thenReturn(mockTemplate);

        // Act
        templateService.deleteTemplate(templateId);

        // Assert
        ArgumentCaptor<WorkflowTemplate> captor = ArgumentCaptor.forClass(WorkflowTemplate.class);
        verify(templateRepository).save(captor.capture());

        WorkflowTemplate captured = captor.getValue();
        assertFalse(captured.getIsActive());
        assertEquals(userId, captured.getUpdatedBy());
        assertNotNull(captured.getUpdatedAt());
    }

    @Test
    void deleteTemplate_WithNonExistentId_ThrowsResourceNotFoundException() {
        // Arrange
        UUID templateId = UUID.randomUUID();
        when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            ResourceNotFoundException.class,
            () -> templateService.deleteTemplate(templateId)
        );

        verify(templateRepository).findById(templateId);
        verify(templateRepository, never()).save(any());
    }

    @Test
    void deleteTemplate_DoesNotHardDelete() {
        // Arrange
        UUID templateId = UUID.randomUUID();
        mockTemplate.setId(templateId);
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));
        when(templateRepository.save(any(WorkflowTemplate.class))).thenReturn(mockTemplate);

        // Act
        templateService.deleteTemplate(templateId);

        // Assert
        verify(templateRepository, never()).delete(any());
        verify(templateRepository, never()).deleteById(any());
        verify(templateRepository).save(any(WorkflowTemplate.class));
    }

    // === HELPER METHODS ===

    private WorkflowTemplate createMockTemplate() {
        WorkflowTemplate template = new WorkflowTemplate();
        template.setId(UUID.randomUUID());
        template.setTemplateName("Standard Onboarding");
        template.setDescription("Default onboarding workflow");
        template.setWorkflowType(WorkflowType.ONBOARDING);
        template.setDefaultStatus(WorkflowStatus.INITIATED);
        template.setIsActive(true);
        template.setCreatedBy(UUID.randomUUID());
        template.setUpdatedBy(UUID.randomUUID());
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());

        // Add mock tasks
        template.getTasks().add(createMockTask("Setup Workspace", 1));
        template.getTasks().add(createMockTask("HR Orientation", 2));

        return template;
    }

    private TemplateTask createMockTask(String name, int sequence) {
        TemplateTask task = new TemplateTask();
        task.setId(UUID.randomUUID());
        task.setTaskName(name);
        task.setDescription("Task description");
        task.setAssignedRole(UserRole.HR_ADMIN);
        task.setSequenceOrder(sequence);
        task.setIsParallel(false);
        task.setCreatedBy(UUID.randomUUID());
        task.setUpdatedBy(UUID.randomUUID());
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return task;
    }

    private CreateTemplateTaskRequest createTaskRequest(String name, UserRole role, int sequence) {
        CreateTemplateTaskRequest request = new CreateTemplateTaskRequest();
        request.setTaskName(name);
        request.setDescription("Task description");
        request.setAssignedRole(role);
        request.setSequenceOrder(sequence);
        request.setIsParallel(false);
        return request;
    }

    // === STORY 2.3: VALIDATION TESTS ===

    @Test
    void createTemplate_WithZeroTasks_ThrowsValidationException() {
        // Arrange
        CreateTemplateRequest request = new CreateTemplateRequest();
        request.setName("Empty Template");
        request.setType(WorkflowType.ONBOARDING);
        request.setTasks(List.of()); // Empty task list

        // Act & Assert
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> templateService.createTemplate(request)
        );

        assertEquals("Template must have at least one task", exception.getMessage());
        verify(templateRepository, never()).save(any());
    }

    @Test
    void createTemplate_WithDuplicateSequenceOrderNonParallel_ThrowsValidationException() {
        // Arrange
        CreateTemplateRequest request = new CreateTemplateRequest();
        request.setName("Template with Duplicate Sequences");
        request.setType(WorkflowType.ONBOARDING);

        CreateTemplateTaskRequest task1 = createTaskRequest("Task 1", UserRole.HR_ADMIN, 1);
        task1.setIsParallel(false);

        CreateTemplateTaskRequest task2 = createTaskRequest("Task 2", UserRole.HR_ADMIN, 1);
        task2.setIsParallel(false); // Same sequence but not parallel

        request.setTasks(List.of(task1, task2));

        // Act & Assert
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> templateService.createTemplate(request)
        );

        assertTrue(exception.getMessage().contains("sequence order 1"));
        assertTrue(exception.getMessage().contains("must be marked as parallel or have unique sequence orders"));
        verify(templateRepository, never()).save(any());
    }

    @Test
    void createTemplate_WithParallelTasks_CreatesSuccessfully() {
        // Arrange
        CreateTemplateRequest request = new CreateTemplateRequest();
        request.setName("Template with Parallel Tasks");
        request.setType(WorkflowType.ONBOARDING);

        CreateTemplateTaskRequest task1 = createTaskRequest("Parallel Task 1", UserRole.HR_ADMIN, 1);
        task1.setIsParallel(true);

        CreateTemplateTaskRequest task2 = createTaskRequest("Parallel Task 2", UserRole.TECH_SUPPORT, 1);
        task2.setIsParallel(true); // Same sequence, both parallel

        CreateTemplateTaskRequest task3 = createTaskRequest("Sequential Task", UserRole.LINE_MANAGER, 2);
        task3.setIsParallel(false);

        request.setTasks(List.of(task1, task2, task3));

        WorkflowTemplate savedTemplate = createMockTemplate();
        when(templateRepository.save(any(WorkflowTemplate.class))).thenReturn(savedTemplate);

        // Act
        TemplateDetailResponse result = templateService.createTemplate(request);

        // Assert
        assertNotNull(result);
        verify(templateRepository).save(any(WorkflowTemplate.class));
    }

    @Test
    void createTemplate_SequenceNormalization_RemovesGaps() {
        // Arrange
        CreateTemplateRequest request = new CreateTemplateRequest();
        request.setName("Template with Gaps");
        request.setType(WorkflowType.ONBOARDING);

        // Create tasks with gaps: 1, 5, 10
        request.setTasks(List.of(
            createTaskRequest("Task 1", UserRole.HR_ADMIN, 1),
            createTaskRequest("Task 2", UserRole.HR_ADMIN, 5),
            createTaskRequest("Task 3", UserRole.HR_ADMIN, 10)
        ));

        WorkflowTemplate savedTemplate = createMockTemplate();
        when(templateRepository.save(any(WorkflowTemplate.class))).thenReturn(savedTemplate);

        // Act
        templateService.createTemplate(request);

        // Assert
        ArgumentCaptor<WorkflowTemplate> captor = ArgumentCaptor.forClass(WorkflowTemplate.class);
        verify(templateRepository).save(captor.capture());

        WorkflowTemplate captured = captor.getValue();
        List<TemplateTask> tasks = captured.getTasks();

        // Should be normalized to 1, 2, 3
        assertEquals(1, tasks.get(0).getSequenceOrder());
        assertEquals(2, tasks.get(1).getSequenceOrder());
        assertEquals(3, tasks.get(2).getSequenceOrder());
    }

    @Test
    void createTemplate_SequenceNormalization_PreservesParallelGrouping() {
        // Arrange
        CreateTemplateRequest request = new CreateTemplateRequest();
        request.setName("Template with Parallel and Gaps");
        request.setType(WorkflowType.ONBOARDING);

        CreateTemplateTaskRequest task1 = createTaskRequest("Task 1", UserRole.HR_ADMIN, 1);
        task1.setIsParallel(true);

        CreateTemplateTaskRequest task2 = createTaskRequest("Task 2", UserRole.HR_ADMIN, 1);
        task2.setIsParallel(true);

        CreateTemplateTaskRequest task3 = createTaskRequest("Task 3", UserRole.HR_ADMIN, 5);
        task3.setIsParallel(false);

        request.setTasks(List.of(task1, task2, task3));

        WorkflowTemplate savedTemplate = createMockTemplate();
        when(templateRepository.save(any(WorkflowTemplate.class))).thenReturn(savedTemplate);

        // Act
        templateService.createTemplate(request);

        // Assert
        ArgumentCaptor<WorkflowTemplate> captor = ArgumentCaptor.forClass(WorkflowTemplate.class);
        verify(templateRepository).save(captor.capture());

        WorkflowTemplate captured = captor.getValue();
        List<TemplateTask> tasks = captured.getTasks();

        // Parallel tasks should remain at sequence 1, solo task should be sequence 2
        assertEquals(1, tasks.get(0).getSequenceOrder());
        assertEquals(1, tasks.get(1).getSequenceOrder());
        assertEquals(2, tasks.get(2).getSequenceOrder());
    }

    @Test
    void updateTemplate_WithZeroTasks_ThrowsValidationException() {
        // Arrange
        UUID templateId = UUID.randomUUID();
        mockTemplate.setId(templateId);
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));

        UpdateTemplateRequest request = new UpdateTemplateRequest();
        request.setName("Updated Template");
        request.setType(WorkflowType.ONBOARDING);
        request.setIsActive(true);
        request.setTasks(List.of()); // Empty

        // Act & Assert
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> templateService.updateTemplate(templateId, request)
        );

        assertEquals("Template must have at least one task", exception.getMessage());
        verify(templateRepository).findById(templateId);
        verify(templateRepository, never()).save(any());
    }

    @Test
    void updateTemplate_WithValidData_NormalizesAndValidates() {
        // Arrange
        UUID templateId = UUID.randomUUID();
        mockTemplate.setId(templateId);
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));
        when(templateRepository.save(any(WorkflowTemplate.class))).thenReturn(mockTemplate);

        UpdateTemplateRequest request = new UpdateTemplateRequest();
        request.setName("Updated Template");
        request.setType(WorkflowType.ONBOARDING);
        request.setIsActive(true);
        request.setTasks(List.of(
            createTaskRequest("Task 1", UserRole.HR_ADMIN, 1),
            createTaskRequest("Task 2", UserRole.HR_ADMIN, 5) // Gap should be normalized
        ));

        // Act
        TemplateDetailResponse result = templateService.updateTemplate(templateId, request);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<WorkflowTemplate> captor = ArgumentCaptor.forClass(WorkflowTemplate.class);
        verify(templateRepository).save(captor.capture());

        WorkflowTemplate captured = captor.getValue();
        List<TemplateTask> tasks = captured.getTasks();

        // Should be normalized to 1, 2
        assertEquals(1, tasks.get(0).getSequenceOrder());
        assertEquals(2, tasks.get(1).getSequenceOrder());
    }
}
