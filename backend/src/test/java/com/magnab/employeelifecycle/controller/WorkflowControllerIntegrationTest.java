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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
