package com.magnab.employeelifecycle.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnab.employeelifecycle.dto.request.InitiateWorkflowRequest;
import com.magnab.employeelifecycle.dto.response.WorkflowInitiationResponse;
import com.magnab.employeelifecycle.entity.*;
import com.magnab.employeelifecycle.enums.TaskStatus;
import com.magnab.employeelifecycle.enums.UserRole;
import com.magnab.employeelifecycle.enums.WorkflowStatus;
import com.magnab.employeelifecycle.enums.WorkflowType;
import com.magnab.employeelifecycle.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for WorkflowController.
 * Tests workflow initiation endpoint with real database using Testcontainers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("WorkflowController Integration Tests")
class WorkflowControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkflowTemplateRepository workflowTemplateRepository;

    @Autowired
    private TemplateTaskRepository templateTaskRepository;

    @Autowired
    private WorkflowInstanceRepository workflowInstanceRepository;

    @Autowired
    private TaskInstanceRepository taskInstanceRepository;

    @Autowired
    private WorkflowStateHistoryRepository workflowStateHistoryRepository;

    private UUID hrAdminUserId;
    private UUID techSupportUserId;
    private UUID activeTemplateId;
    private UUID inactiveTemplateId;

    @BeforeEach
    void setUp() {
        // Seed HR_ADMIN user for workflow initiation tests
        if (userRepository.findByUsername("hradmin").isEmpty()) {
            User hrAdmin = new User();
            hrAdmin.setUsername("hradmin");
            hrAdmin.setEmail("hradmin@test.magnab.com");
            hrAdmin.setPasswordHash("$2a$10$dummyHashForTestingOnly");
            hrAdmin.setRole(UserRole.HR_ADMIN);
            hrAdmin.setIsActive(true);
            hrAdmin.setCreatedAt(LocalDateTime.now());
            hrAdmin = userRepository.save(hrAdmin);
            hrAdminUserId = hrAdmin.getId();
        } else {
            hrAdminUserId = userRepository.findByUsername("hradmin").get().getId();
        }

        // Seed TECH_SUPPORT user for authorization tests
        if (userRepository.findByUsername("techsupport").isEmpty()) {
            User techSupport = new User();
            techSupport.setUsername("techsupport");
            techSupport.setEmail("techsupport@test.magnab.com");
            techSupport.setPasswordHash("$2a$10$dummyHashForTestingOnly");
            techSupport.setRole(UserRole.TECH_SUPPORT);
            techSupport.setIsActive(true);
            techSupport.setCreatedAt(LocalDateTime.now());
            techSupport = userRepository.save(techSupport);
            techSupportUserId = techSupport.getId();
        } else {
            techSupportUserId = userRepository.findByUsername("techsupport").get().getId();
        }

        // Create active workflow template for testing (only if not exists)
        List<WorkflowTemplate> existingActiveTemplates = workflowTemplateRepository
                .findByTemplateNameAndIsActive("Test Onboarding Template", true);

        WorkflowTemplate activeTemplate;
        if (existingActiveTemplates.isEmpty()) {
            activeTemplate = new WorkflowTemplate();
            activeTemplate.setTemplateName("Test Onboarding Template");
            activeTemplate.setWorkflowType(WorkflowType.ONBOARDING);
            activeTemplate.setIsActive(true);
            activeTemplate.setCreatedBy(hrAdminUserId);
            activeTemplate.setCreatedAt(LocalDateTime.now());
            activeTemplate = workflowTemplateRepository.save(activeTemplate);

            // Add tasks to active template
            TemplateTask task1 = new TemplateTask();
            task1.setTemplate(activeTemplate);
            task1.setTaskName("Setup email account");
            task1.setAssignedRole(UserRole.TECH_SUPPORT);
            task1.setSequenceOrder(1);
            task1.setCreatedBy(hrAdminUserId);
            task1.setCreatedAt(LocalDateTime.now());
            templateTaskRepository.save(task1);

            TemplateTask task2 = new TemplateTask();
            task2.setTemplate(activeTemplate);
            task2.setTaskName("Complete HR paperwork");
            task2.setAssignedRole(UserRole.HR_ADMIN);
            task2.setSequenceOrder(2);
            task2.setCreatedBy(hrAdminUserId);
            task2.setCreatedAt(LocalDateTime.now());
            templateTaskRepository.save(task2);
        } else {
            activeTemplate = existingActiveTemplates.get(0);
        }
        activeTemplateId = activeTemplate.getId();

        // Create inactive template for testing validation (only if not exists)
        List<WorkflowTemplate> existingInactiveTemplates = workflowTemplateRepository
                .findByTemplateNameAndIsActive("Inactive Template", false);

        WorkflowTemplate inactiveTemplate;
        if (existingInactiveTemplates.isEmpty()) {
            inactiveTemplate = new WorkflowTemplate();
            inactiveTemplate.setTemplateName("Inactive Template");
            inactiveTemplate.setWorkflowType(WorkflowType.ONBOARDING);
            inactiveTemplate.setIsActive(false);
            inactiveTemplate.setCreatedBy(hrAdminUserId);
            inactiveTemplate.setCreatedAt(LocalDateTime.now());
            inactiveTemplate = workflowTemplateRepository.save(inactiveTemplate);
        } else {
            inactiveTemplate = existingInactiveTemplates.get(0);
        }
        inactiveTemplateId = inactiveTemplate.getId();
    }

    /**
     * Helper method to set up SecurityContext with a proper User entity as principal.
     * This is needed because the controller expects the authentication principal to be a User object.
     */
    private void setupSecurityContext(UUID userId, String username, UserRole role) {
        User user = userRepository.findById(userId).orElseThrow();

        org.springframework.security.core.Authentication authentication =
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                user,
                null,
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role.name()))
            );

        org.springframework.security.core.context.SecurityContext securityContext =
            org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Should create workflow when valid request with HR_ADMIN role")
    void initiateWorkflow_ValidRequestWithHRAdmin_Returns201() throws Exception {
        setupSecurityContext(hrAdminUserId, "hradmin", UserRole.HR_ADMIN);
        // Arrange
        InitiateWorkflowRequest request = new InitiateWorkflowRequest();
        request.setTemplateId(activeTemplateId);
        request.setEmployeeName("John Doe");
        request.setEmployeeEmail("john.doe@company.com");
        request.setEmployeeRole("Software Engineer");
        Map<String, Object> customFields = new HashMap<>();
        customFields.put("startDate", "2025-02-01");
        customFields.put("remoteStatus", "hybrid");
        request.setCustomFieldValues(customFields);

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/workflows")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workflowInstanceId").exists())
                .andExpect(jsonPath("$.employeeName").value("John Doe"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.totalTasksCreated").value(2))
                .andExpect(jsonPath("$.tasksAssigned").isNumber())
                .andExpect(jsonPath("$.initiatedAt").exists())
                .andReturn();

        // Verify workflow created in database
        WorkflowInitiationResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                WorkflowInitiationResponse.class
        );

        WorkflowInstance createdWorkflow = workflowInstanceRepository
                .findById(response.getWorkflowInstanceId())
                .orElseThrow();

        assertThat(createdWorkflow.getEmployeeName()).isEqualTo("John Doe");
        assertThat(createdWorkflow.getEmployeeEmail()).isEqualTo("john.doe@company.com");
        assertThat(createdWorkflow.getStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);

        // Verify tasks created
        List<TaskInstance> tasks = taskInstanceRepository
                .findByWorkflowInstanceId(response.getWorkflowInstanceId());
        assertThat(tasks).hasSize(2);
        assertThat(tasks).allMatch(t -> t.getStatus() == TaskStatus.IN_PROGRESS); // All assigned

        // Verify workflow state history created
        List<WorkflowStateHistory> history = workflowStateHistoryRepository
                .findByWorkflowInstanceIdOrderByChangedAtDesc(response.getWorkflowInstanceId());
        assertThat(history).hasSizeGreaterThanOrEqualTo(2); // INITIATED + IN_PROGRESS transitions
    }

    @Test
    @DisplayName("Should return 403 when non-HR_ADMIN tries to initiate workflow")
    void initiateWorkflow_WithTechSupportRole_Returns403() throws Exception {
        setupSecurityContext(techSupportUserId, "techsupport", UserRole.TECH_SUPPORT);
        // Arrange
        InitiateWorkflowRequest request = new InitiateWorkflowRequest();
        request.setTemplateId(activeTemplateId);
        request.setEmployeeName("Jane Smith");
        request.setEmployeeEmail("jane.smith@company.com");
        request.setEmployeeRole("Engineer");

        // Act & Assert
        mockMvc.perform(post("/api/workflows")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Verify no workflow created
        List<WorkflowInstance> workflows = workflowInstanceRepository.findAll();
        long janeWorkflows = workflows.stream()
                .filter(w -> "Jane Smith".equals(w.getEmployeeName()))
                .count();
        assertThat(janeWorkflows).isZero();
    }

    @Test
    @DisplayName("Should return 404 when template not found")
    void initiateWorkflow_TemplateNotFound_Returns404() throws Exception {
        setupSecurityContext(hrAdminUserId, "hradmin", UserRole.HR_ADMIN);
        // Arrange
        UUID nonExistentTemplateId = UUID.randomUUID();
        InitiateWorkflowRequest request = new InitiateWorkflowRequest();
        request.setTemplateId(nonExistentTemplateId);
        request.setEmployeeName("Test User");
        request.setEmployeeEmail("test@company.com");
        request.setEmployeeRole("Engineer");

        // Act & Assert
        mockMvc.perform(post("/api/workflows")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    @DisplayName("Should return 400 when template is inactive")
    void initiateWorkflow_InactiveTemplate_Returns400() throws Exception {
        setupSecurityContext(hrAdminUserId, "hradmin", UserRole.HR_ADMIN);
        // Arrange
        InitiateWorkflowRequest request = new InitiateWorkflowRequest();
        request.setTemplateId(inactiveTemplateId);
        request.setEmployeeName("Test User");
        request.setEmployeeEmail("test@company.com");
        request.setEmployeeRole("Engineer");

        // Act & Assert
        mockMvc.perform(post("/api/workflows")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("inactive")));
    }

    @Test
    @DisplayName("Should return 400 when required fields are missing")
    void initiateWorkflow_MissingRequiredFields_Returns400() throws Exception {
        setupSecurityContext(hrAdminUserId, "hradmin", UserRole.HR_ADMIN);
        // Arrange - missing employee name
        InitiateWorkflowRequest request = new InitiateWorkflowRequest();
        request.setTemplateId(activeTemplateId);
        request.setEmployeeEmail("test@company.com");
        request.setEmployeeRole("Engineer");

        // Act & Assert
        mockMvc.perform(post("/api/workflows")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when email format is invalid")
    void initiateWorkflow_InvalidEmailFormat_Returns400() throws Exception {
        setupSecurityContext(hrAdminUserId, "hradmin", UserRole.HR_ADMIN);
        // Arrange
        InitiateWorkflowRequest request = new InitiateWorkflowRequest();
        request.setTemplateId(activeTemplateId);
        request.setEmployeeName("Test User");
        request.setEmployeeEmail("invalid-email");
        request.setEmployeeRole("Engineer");

        // Act & Assert
        mockMvc.perform(post("/api/workflows")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle null custom field values")
    void initiateWorkflow_NullCustomFieldValues_Returns201() throws Exception {
        setupSecurityContext(hrAdminUserId, "hradmin", UserRole.HR_ADMIN);
        // Arrange
        InitiateWorkflowRequest request = new InitiateWorkflowRequest();
        request.setTemplateId(activeTemplateId);
        request.setEmployeeName("Bob Johnson");
        request.setEmployeeEmail("bob.johnson@company.com");
        request.setEmployeeRole("Manager");
        request.setCustomFieldValues(null);

        // Act & Assert
        mockMvc.perform(post("/api/workflows")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeName").value("Bob Johnson"));
    }

    // ========== GET /api/workflows Tests ==========

    @Test
    @DisplayName("GET /api/workflows - HR_ADMIN should retrieve all workflows with pagination")
    void getWorkflows_HrAdmin_ReturnsAllWorkflows() throws Exception {
        setupSecurityContext(hrAdminUserId, "hradmin", UserRole.HR_ADMIN);

        // Create some test workflows
        WorkflowInstance workflow1 = createTestWorkflow("Alice Adams", "alice@company.com", WorkflowType.ONBOARDING);
        WorkflowInstance workflow2 = createTestWorkflow("Bob Brown", "bob@company.com", WorkflowType.OFFBOARDING);

        // Act & Assert
        mockMvc.perform(get("/api/workflows")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.content[*].employeeName", hasItems("Alice Adams", "Bob Brown")))
                .andExpect(jsonPath("$.content[*].workflowType", hasItems("ONBOARDING", "OFFBOARDING")))
                .andExpect(jsonPath("$.content[0].totalTasks").exists())
                .andExpect(jsonPath("$.content[0].completedTasks").exists());
    }

    @Test
    @DisplayName("GET /api/workflows - Should apply status filter correctly")
    void getWorkflows_WithStatusFilter_ReturnsFilteredWorkflows() throws Exception {
        setupSecurityContext(hrAdminUserId, "hradmin", UserRole.HR_ADMIN);

        // Create workflows with different statuses
        WorkflowInstance inProgressWorkflow = createTestWorkflow("Charlie Chen", "charlie@company.com", WorkflowType.ONBOARDING);
        WorkflowInstance completedWorkflow = createTestWorkflow("Diana Davis", "diana@company.com", WorkflowType.ONBOARDING);
        completedWorkflow.setStatus(WorkflowStatus.COMPLETED);
        completedWorkflow.setCompletedAt(LocalDateTime.now());
        workflowInstanceRepository.save(completedWorkflow);

        // Act & Assert - Filter by IN_PROGRESS
        mockMvc.perform(get("/api/workflows")
                        .param("status", "IN_PROGRESS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].status").value(everyItem(is("IN_PROGRESS"))));

        // Filter by COMPLETED
        mockMvc.perform(get("/api/workflows")
                        .param("status", "COMPLETED")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].status").value(everyItem(is("COMPLETED"))));
    }

    @Test
    @DisplayName("GET /api/workflows - Should apply workflow type filter correctly")
    void getWorkflows_WithWorkflowTypeFilter_ReturnsFilteredWorkflows() throws Exception {
        setupSecurityContext(hrAdminUserId, "hradmin", UserRole.HR_ADMIN);

        // Create workflows with different types
        createTestWorkflow("Eve Ellis", "eve@company.com", WorkflowType.ONBOARDING);
        createTestWorkflow("Frank Ford", "frank@company.com", WorkflowType.OFFBOARDING);

        // Act & Assert - Filter by ONBOARDING
        mockMvc.perform(get("/api/workflows")
                        .param("workflowType", "ONBOARDING")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].workflowType").value(everyItem(is("ONBOARDING"))));

        // Filter by OFFBOARDING
        mockMvc.perform(get("/api/workflows")
                        .param("workflowType", "OFFBOARDING")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].workflowType").value(everyItem(is("OFFBOARDING"))));
    }

    @Test
    @DisplayName("GET /api/workflows - Should search by employee name")
    void getWorkflows_WithEmployeeNameSearch_ReturnsMatchingWorkflows() throws Exception {
        setupSecurityContext(hrAdminUserId, "hradmin", UserRole.HR_ADMIN);

        // Create workflows
        createTestWorkflow("Grace Green", "grace@company.com", WorkflowType.ONBOARDING);
        createTestWorkflow("Henry Harris", "henry@company.com", WorkflowType.ONBOARDING);

        // Act & Assert - Search for "Grace"
        mockMvc.perform(get("/api/workflows")
                        .param("employeeName", "Grace")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].employeeName", everyItem(containsStringIgnoringCase("Grace"))));
    }

    @Test
    @DisplayName("GET /api/workflows - Should apply pagination correctly")
    void getWorkflows_WithPagination_ReturnsPaginatedResults() throws Exception {
        setupSecurityContext(hrAdminUserId, "hradmin", UserRole.HR_ADMIN);

        // Create several workflows
        for (int i = 1; i <= 5; i++) {
            createTestWorkflow("Employee" + i, "emp" + i + "@company.com", WorkflowType.ONBOARDING);
        }

        // Act & Assert - Page 0, size 2
        mockMvc.perform(get("/api/workflows")
                        .param("page", "0")
                        .param("size", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(5)));
    }

    @Test
    @DisplayName("GET /api/workflows - Non-admin should see only their workflows")
    void getWorkflows_NonAdmin_ReturnsOnlyAssignedWorkflows() throws Exception {
        setupSecurityContext(techSupportUserId, "techsupport", UserRole.TECH_SUPPORT);

        // Create workflow and assign task to tech support user
        WorkflowInstance workflow = createTestWorkflow("Iris Iverson", "iris@company.com", WorkflowType.ONBOARDING);

        // Assign a task to the tech support user
        TaskInstance task = createTaskForWorkflow(workflow.getId(), "Setup workstation", UserRole.TECH_SUPPORT, 1);
        task.setAssignedUserId(techSupportUserId);
        taskInstanceRepository.save(task);

        // Create another workflow that tech support is NOT involved in
        createTestWorkflow("Jack Johnson", "jack@company.com", WorkflowType.ONBOARDING);

        // Act & Assert - Should only see Iris's workflow
        mockMvc.perform(get("/api/workflows")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].employeeName", hasItem("Iris Iverson")))
                .andExpect(jsonPath("$.content[*].employeeName", not(hasItem("Jack Johnson"))));
    }

    @Test
    @DisplayName("GET /api/workflows - Should return 401 when not authenticated")
    void getWorkflows_NotAuthenticated_Returns401() throws Exception {
        // Clear security context
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        // Act & Assert
        mockMvc.perform(get("/api/workflows")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // ========== GET /api/workflows/{id} Tests ==========

    @Test
    @DisplayName("GET /api/workflows/{id} - HR_ADMIN can view any workflow details")
    void getWorkflowById_HrAdmin_ReturnsWorkflowDetails() throws Exception {
        setupSecurityContext(hrAdminUserId, "hradmin", UserRole.HR_ADMIN);

        // Create test workflow
        WorkflowInstance workflow = createTestWorkflow("Kelly King", "kelly@company.com", WorkflowType.ONBOARDING);

        // Create some tasks for the workflow
        TaskInstance task1 = createTaskForWorkflow(workflow.getId(), "Setup email", UserRole.TECH_SUPPORT, 1);
        TaskInstance task2 = createTaskForWorkflow(workflow.getId(), "HR paperwork", UserRole.HR_ADMIN, 2);

        // Create state history
        WorkflowStateHistory history = new WorkflowStateHistory();
        history.setWorkflowInstanceId(workflow.getId());
        history.setPreviousStatus(WorkflowStatus.INITIATED);
        history.setNewStatus(WorkflowStatus.IN_PROGRESS);
        history.setChangedBy(hrAdminUserId);
        history.setChangedAt(LocalDateTime.now());
        history.setNotes("Workflow started");
        workflowStateHistoryRepository.save(history);

        // Act & Assert
        mockMvc.perform(get("/api/workflows/{id}", workflow.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workflow.getId().toString()))
                .andExpect(jsonPath("$.employeeName").value("Kelly King"))
                .andExpect(jsonPath("$.employeeEmail").value("kelly@company.com"))
                .andExpect(jsonPath("$.workflowType").value("ONBOARDING"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.tasks").isArray())
                .andExpect(jsonPath("$.tasks", hasSize(2)))
                .andExpect(jsonPath("$.tasks[0].taskName").exists())
                .andExpect(jsonPath("$.stateHistory").isArray())
                .andExpect(jsonPath("$.stateHistory", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.customFieldValues").exists());
    }

    @Test
    @DisplayName("GET /api/workflows/{id} - Non-admin can view workflow they're involved in")
    void getWorkflowById_AuthorizedNonAdmin_ReturnsWorkflowDetails() throws Exception {
        setupSecurityContext(techSupportUserId, "techsupport", UserRole.TECH_SUPPORT);

        // Create workflow
        WorkflowInstance workflow = createTestWorkflow("Laura Lee", "laura@company.com", WorkflowType.ONBOARDING);

        // Assign task to tech support user
        TaskInstance task = createTaskForWorkflow(workflow.getId(), "Setup laptop", UserRole.TECH_SUPPORT, 1);
        task.setAssignedUserId(techSupportUserId);
        taskInstanceRepository.save(task);

        // Act & Assert
        mockMvc.perform(get("/api/workflows/{id}", workflow.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workflow.getId().toString()))
                .andExpect(jsonPath("$.employeeName").value("Laura Lee"));
    }

    @Test
    @DisplayName("GET /api/workflows/{id} - Non-admin cannot view workflow they're not involved in")
    void getWorkflowById_UnauthorizedNonAdmin_Returns403() throws Exception {
        setupSecurityContext(techSupportUserId, "techsupport", UserRole.TECH_SUPPORT);

        // Create workflow that tech support is NOT involved in
        WorkflowInstance workflow = createTestWorkflow("Mark Miller", "mark@company.com", WorkflowType.ONBOARDING);

        // Create task assigned to different user
        TaskInstance task = createTaskForWorkflow(workflow.getId(), "HR task", UserRole.HR_ADMIN, 1);
        task.setAssignedUserId(hrAdminUserId);
        taskInstanceRepository.save(task);

        // Act & Assert
        mockMvc.perform(get("/api/workflows/{id}", workflow.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsStringIgnoringCase("not authorized")));
    }

    @Test
    @DisplayName("GET /api/workflows/{id} - Should return 404 when workflow not found")
    void getWorkflowById_WorkflowNotFound_Returns404() throws Exception {
        setupSecurityContext(hrAdminUserId, "hradmin", UserRole.HR_ADMIN);

        UUID nonExistentId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(get("/api/workflows/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsStringIgnoringCase("not found")));
    }

    @Test
    @DisplayName("GET /api/workflows/{id} - Should return 401 when not authenticated")
    void getWorkflowById_NotAuthenticated_Returns401() throws Exception {
        // Create workflow
        WorkflowInstance workflow = createTestWorkflow("Nancy Nash", "nancy@company.com", WorkflowType.ONBOARDING);

        // Clear security context
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        // Act & Assert
        mockMvc.perform(get("/api/workflows/{id}", workflow.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/workflows/{id} - Should include complete custom field values")
    void getWorkflowById_IncludesCustomFieldValues() throws Exception {
        setupSecurityContext(hrAdminUserId, "hradmin", UserRole.HR_ADMIN);

        // Create workflow with custom fields
        WorkflowInstance workflow = createTestWorkflow("Oliver Owen", "oliver@company.com", WorkflowType.ONBOARDING);
        Map<String, Object> customFields = new HashMap<>();
        customFields.put("startDate", "2025-03-01");
        customFields.put("department", "Engineering");
        customFields.put("remoteStatus", "remote");
        workflow.setCustomFieldValues(customFields);
        workflowInstanceRepository.save(workflow);

        // Act & Assert
        mockMvc.perform(get("/api/workflows/{id}", workflow.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customFieldValues.startDate").value("2025-03-01"))
                .andExpect(jsonPath("$.customFieldValues.department").value("Engineering"))
                .andExpect(jsonPath("$.customFieldValues.remoteStatus").value("remote"));
    }

    // ========== Helper Methods ==========

    private WorkflowInstance createTestWorkflow(String employeeName, String employeeEmail, WorkflowType workflowType) {
        WorkflowInstance workflow = new WorkflowInstance();
        workflow.setTemplateId(activeTemplateId);
        workflow.setEmployeeName(employeeName);
        workflow.setEmployeeEmail(employeeEmail);
        workflow.setEmployeeRole("Software Engineer");
        workflow.setWorkflowType(workflowType);
        workflow.setStatus(WorkflowStatus.IN_PROGRESS);
        workflow.setInitiatedBy(hrAdminUserId);
        workflow.setInitiatedAt(LocalDateTime.now());
        workflow.setCustomFieldValues(new HashMap<>());
        return workflowInstanceRepository.save(workflow);
    }

    private TaskInstance createTaskForWorkflow(UUID workflowId, String taskName, UserRole assignedRole, int sequenceOrder) {
        // Get a template task ID from the active template (use the first one)
        List<TemplateTask> templateTasks = templateTaskRepository.findByTemplateIdOrderBySequenceOrder(activeTemplateId);
        UUID templateTaskId = templateTasks.isEmpty() ? null : templateTasks.get(0).getId();

        TaskInstance task = new TaskInstance();
        task.setWorkflowInstanceId(workflowId);
        task.setTaskName(taskName);
        task.setAssignedRole(assignedRole);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setSequenceOrder(sequenceOrder);
        task.setIsVisible(true);
        task.setTemplateTaskId(templateTaskId); // Use real template task ID
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return taskInstanceRepository.save(task);
    }
}
