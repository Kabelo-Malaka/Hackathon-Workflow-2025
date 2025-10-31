package com.magnab.employeelifecycle.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnab.employeelifecycle.dto.request.CreateTemplateRequest;
import com.magnab.employeelifecycle.dto.request.CreateTemplateTaskRequest;
import com.magnab.employeelifecycle.dto.request.UpdateTemplateRequest;
import com.magnab.employeelifecycle.dto.response.TemplateDetailResponse;
import com.magnab.employeelifecycle.entity.User;
import com.magnab.employeelifecycle.enums.UserRole;
import com.magnab.employeelifecycle.enums.WorkflowType;
import com.magnab.employeelifecycle.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for TemplateController.
 * Tests all CRUD operations with real database using Testcontainers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TemplateControllerIntegrationTest {

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

    @BeforeEach
    void setUp() throws Exception {
        // Seed admin user in test database for getCurrentUser() authentication lookup
        // This fixes the issue where @WithMockUser creates mock security context
        // but TemplateService.getCurrentUser() queries the actual database
        if (userRepository.findByUsername("admin").isEmpty()) {
            User adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setEmail("admin@test.magnab.com");
            adminUser.setPasswordHash("$2a$10$dummyHashForTestingOnly");
            adminUser.setRole(UserRole.ADMINISTRATOR);
            adminUser.setIsActive(true);
            adminUser.setCreatedAt(LocalDateTime.now());
            userRepository.save(adminUser);
        }

        // Seed techsupport user for role-based authorization tests
        if (userRepository.findByUsername("techsupport").isEmpty()) {
            User techSupportUser = new User();
            techSupportUser.setUsername("techsupport");
            techSupportUser.setEmail("techsupport@test.magnab.com");
            techSupportUser.setPasswordHash("$2a$10$dummyHashForTestingOnly");
            techSupportUser.setRole(UserRole.TECH_SUPPORT);
            techSupportUser.setIsActive(true);
            techSupportUser.setCreatedAt(LocalDateTime.now());
            userRepository.save(techSupportUser);
        }
    }

    // === CREATE TEMPLATE TESTS ===

    @Test
    @WithMockUser(username = "admin", roles = {"HR_ADMIN"})
    void createTemplate_WithValidData_Returns201() throws Exception {
        CreateTemplateRequest request = createValidTemplateRequest("Onboarding Template");

        mockMvc.perform(post("/api/templates")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("Onboarding Template"))
            .andExpect(jsonPath("$.type").value("ONBOARDING"))
            .andExpect(jsonPath("$.isActive").value(true))
            .andExpect(jsonPath("$.tasks").isArray())
            .andExpect(jsonPath("$.tasks", hasSize(2)))
            .andExpect(jsonPath("$.tasks[0].taskName").value("Task 1"))
            .andExpect(jsonPath("$.tasks[0].assignedRole").value("HR_ADMIN"))
            .andExpect(jsonPath("$.tasks[1].taskName").value("Task 2"))
            .andExpect(jsonPath("$.createdBy").exists())
            .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMINISTRATOR"})
    void createTemplate_WithAdministratorRole_Returns201() throws Exception {
        CreateTemplateRequest request = createValidTemplateRequest("Admin Template");

        mockMvc.perform(post("/api/templates")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Admin Template"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"HR_ADMIN"})
    void createTemplate_WithMissingName_Returns400() throws Exception {
        CreateTemplateRequest request = createValidTemplateRequest(null);

        mockMvc.perform(post("/api/templates")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"HR_ADMIN"})
    void createTemplate_WithEmptyTasks_Returns400() throws Exception {
        CreateTemplateRequest request = new CreateTemplateRequest();
        request.setName("Template with no tasks");
        request.setType(WorkflowType.ONBOARDING);
        request.setTasks(List.of());

        mockMvc.perform(post("/api/templates")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createTemplate_WithoutAuthentication_Returns401() throws Exception {
        CreateTemplateRequest request = createValidTemplateRequest("Test Template");

        mockMvc.perform(post("/api/templates")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000", roles = {"LINE_MANAGER"})
    void createTemplate_WithoutHRAdminRole_Returns403() throws Exception {
        CreateTemplateRequest request = createValidTemplateRequest("Test Template");

        mockMvc.perform(post("/api/templates")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    // === GET ALL TEMPLATES TESTS ===

    @Test
    @WithMockUser(username = "admin", roles = {"HR_ADMIN"})
    void getAllTemplates_ReturnsTemplateList() throws Exception {
        mockMvc.perform(get("/api/templates"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(0)))) // List may be empty or contain seed templates
            .andExpect(jsonPath("$").exists());
    }

    @Test
    void getAllTemplates_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/templates"))
            .andExpect(status().isUnauthorized());
    }

    // === GET TEMPLATE BY ID TESTS ===

    @Test
    @WithMockUser(username = "admin", roles = {"HR_ADMIN"})
    void getTemplateById_WithExistingId_Returns200() throws Exception {
        // Use seed data template ID
        UUID templateId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        mockMvc.perform(get("/api/templates/{id}", templateId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(templateId.toString()))
            .andExpect(jsonPath("$.name").exists())
            .andExpect(jsonPath("$.type").exists())
            .andExpect(jsonPath("$.tasks").isArray())
            .andExpect(jsonPath("$.tasks", hasSize(greaterThan(0))))
            .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"HR_ADMIN"})
    void getTemplateById_WithNonExistentId_Returns404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/templates/{id}", nonExistentId))
            .andExpect(status().isNotFound());
    }

    @Test
    void getTemplateById_WithoutAuthentication_Returns401() throws Exception {
        UUID templateId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        mockMvc.perform(get("/api/templates/{id}", templateId))
            .andExpect(status().isUnauthorized());
    }

    // === UPDATE TEMPLATE TESTS ===

    @Test
    @WithMockUser(username = "admin", roles = {"HR_ADMIN"})
    void updateTemplate_WithValidData_Returns200() throws Exception {
        // First create a template
        CreateTemplateRequest createRequest = createValidTemplateRequest("Original Template");
        MvcResult createResult = mockMvc.perform(post("/api/templates")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        TemplateDetailResponse created = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            TemplateDetailResponse.class
        );

        // Now update it
        UpdateTemplateRequest updateRequest = new UpdateTemplateRequest();
        updateRequest.setName("Updated Template");
        updateRequest.setDescription("Updated description");
        updateRequest.setType(WorkflowType.OFFBOARDING);
        updateRequest.setIsActive(true);
        updateRequest.setTasks(List.of(
            createTaskRequest("Updated Task 1", UserRole.HR_ADMIN, 1, false, null)
        ));

        mockMvc.perform(put("/api/templates/{id}", created.getId())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(created.getId().toString()))
            .andExpect(jsonPath("$.name").value("Updated Template"))
            .andExpect(jsonPath("$.description").value("Updated description"))
            .andExpect(jsonPath("$.type").value("OFFBOARDING"))
            .andExpect(jsonPath("$.tasks", hasSize(1)))
            .andExpect(jsonPath("$.tasks[0].taskName").value("Updated Task 1"))
            .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"HR_ADMIN"})
    void updateTemplate_SetInactive_Returns200() throws Exception {
        // Create template first
        CreateTemplateRequest createRequest = createValidTemplateRequest("Template to Deactivate");
        MvcResult createResult = mockMvc.perform(post("/api/templates")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        TemplateDetailResponse created = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            TemplateDetailResponse.class
        );

        // Update to inactive
        UpdateTemplateRequest updateRequest = new UpdateTemplateRequest();
        updateRequest.setName(created.getName());
        updateRequest.setType(created.getType());
        updateRequest.setIsActive(false);
        updateRequest.setTasks(List.of(
            createTaskRequest("Task 1", UserRole.HR_ADMIN, 1, false, null)
        ));

        mockMvc.perform(put("/api/templates/{id}", created.getId())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"HR_ADMIN"})
    void updateTemplate_WithNonExistentId_Returns404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        UpdateTemplateRequest updateRequest = new UpdateTemplateRequest();
        updateRequest.setName("Test");
        updateRequest.setType(WorkflowType.ONBOARDING);
        updateRequest.setIsActive(true);
        updateRequest.setTasks(List.of(
            createTaskRequest("Task 1", UserRole.HR_ADMIN, 1, false, null)
        ));

        mockMvc.perform(put("/api/templates/{id}", nonExistentId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateTemplate_WithoutAuthentication_Returns401() throws Exception {
        UUID templateId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        UpdateTemplateRequest updateRequest = new UpdateTemplateRequest();
        updateRequest.setName("Test");
        updateRequest.setType(WorkflowType.ONBOARDING);
        updateRequest.setIsActive(true);
        updateRequest.setTasks(List.of(
            createTaskRequest("Task 1", UserRole.HR_ADMIN, 1, false, null)
        ));

        mockMvc.perform(put("/api/templates/{id}", templateId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isUnauthorized());
    }

    // === DELETE TEMPLATE TESTS ===

    @Test
    @WithMockUser(username = "admin", roles = {"HR_ADMIN"})
    void deleteTemplate_WithExistingId_Returns204() throws Exception {
        // Create template first
        CreateTemplateRequest createRequest = createValidTemplateRequest("Template to Delete");
        MvcResult createResult = mockMvc.perform(post("/api/templates")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        TemplateDetailResponse created = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            TemplateDetailResponse.class
        );

        // Delete it
        mockMvc.perform(delete("/api/templates/{id}", created.getId())
                .with(csrf()))
            .andExpect(status().isNoContent());

        // Verify it's soft deleted (is_active = false)
        mockMvc.perform(get("/api/templates/{id}", created.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"HR_ADMIN"})
    void deleteTemplate_WithNonExistentId_Returns404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(delete("/api/templates/{id}", nonExistentId)
                .with(csrf()))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteTemplate_WithoutAuthentication_Returns401() throws Exception {
        UUID templateId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        mockMvc.perform(delete("/api/templates/{id}", templateId)
                .with(csrf()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "techsupport", roles = {"TECH_SUPPORT"})
    void deleteTemplate_WithoutHRAdminRole_Returns403() throws Exception {
        UUID templateId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        mockMvc.perform(delete("/api/templates/{id}", templateId)
                .with(csrf()))
            .andExpect(status().isForbidden());
    }

    // === STORY 2.3: VALIDATION INTEGRATION TESTS ===

    @Test
    @WithMockUser(username = "admin", roles = {"HR_ADMIN"})
    void createTemplate_WithZeroTasks_Returns400() throws Exception {
        CreateTemplateRequest request = new CreateTemplateRequest();
        request.setName("Empty Template");
        request.setType(WorkflowType.ONBOARDING);
        request.setTasks(List.of()); // Empty task list

        mockMvc.perform(post("/api/templates")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Template must have at least one task"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"HR_ADMIN"})
    void createTemplate_WithDuplicateSequenceNonParallel_Returns400() throws Exception {
        CreateTemplateRequest request = new CreateTemplateRequest();
        request.setName("Template with Duplicate Sequences");
        request.setType(WorkflowType.ONBOARDING);
        request.setTasks(List.of(
            createTaskRequest("Task 1", UserRole.HR_ADMIN, 1, false, null),
            createTaskRequest("Task 2", UserRole.HR_ADMIN, 1, false, null) // Duplicate sequence, not parallel
        ));

        mockMvc.perform(post("/api/templates")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("sequence order 1")))
            .andExpect(jsonPath("$.message").value(containsString("must be marked as parallel or have unique sequence orders")));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"HR_ADMIN"})
    void createTemplate_WithValidParallelTasks_Returns201() throws Exception {
        CreateTemplateRequest request = new CreateTemplateRequest();
        request.setName("Template with Parallel Tasks");
        request.setType(WorkflowType.ONBOARDING);
        request.setTasks(List.of(
            createTaskRequest("Parallel Task 1", UserRole.HR_ADMIN, 1, true, null),
            createTaskRequest("Parallel Task 2", UserRole.TECH_SUPPORT, 1, true, null),
            createTaskRequest("Sequential Task", UserRole.LINE_MANAGER, 2, false, null)
        ));

        mockMvc.perform(post("/api/templates")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tasks", hasSize(3)))
            .andExpect(jsonPath("$.tasks[0].sequenceOrder").value(1))
            .andExpect(jsonPath("$.tasks[1].sequenceOrder").value(1))
            .andExpect(jsonPath("$.tasks[2].sequenceOrder").value(2));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"HR_ADMIN"})
    void createTemplate_SequenceNormalization_RemovesGaps() throws Exception {
        CreateTemplateRequest request = new CreateTemplateRequest();
        request.setName("Template with Gaps");
        request.setType(WorkflowType.ONBOARDING);
        request.setTasks(List.of(
            createTaskRequest("Task 1", UserRole.HR_ADMIN, 1, false, null),
            createTaskRequest("Task 2", UserRole.HR_ADMIN, 5, false, null),
            createTaskRequest("Task 3", UserRole.HR_ADMIN, 10, false, null)
        ));

        mockMvc.perform(post("/api/templates")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tasks", hasSize(3)))
            .andExpect(jsonPath("$.tasks[0].sequenceOrder").value(1))
            .andExpect(jsonPath("$.tasks[1].sequenceOrder").value(2))
            .andExpect(jsonPath("$.tasks[2].sequenceOrder").value(3));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"HR_ADMIN"})
    void updateTemplate_WithZeroTasks_Returns400() throws Exception {
        // First create a template
        CreateTemplateRequest createRequest = createValidTemplateRequest("Template for Zero Tasks Test " + System.currentTimeMillis());
        MvcResult createResult = mockMvc.perform(post("/api/templates")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        TemplateDetailResponse created = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            TemplateDetailResponse.class
        );

        // Try to update with zero tasks
        UpdateTemplateRequest updateRequest = new UpdateTemplateRequest();
        updateRequest.setName("Updated Template");
        updateRequest.setType(WorkflowType.ONBOARDING);
        updateRequest.setIsActive(true);
        updateRequest.setTasks(List.of()); // Empty

        mockMvc.perform(put("/api/templates/{id}", created.getId())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Template must have at least one task"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"HR_ADMIN"})
    void updateTemplate_WithValidationErrors_Returns400() throws Exception {
        // First create a template
        CreateTemplateRequest createRequest = createValidTemplateRequest("Template for Validation Errors Test " + System.currentTimeMillis());
        MvcResult createResult = mockMvc.perform(post("/api/templates")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        TemplateDetailResponse created = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            TemplateDetailResponse.class
        );

        // Try to update with duplicate sequences (non-parallel)
        UpdateTemplateRequest updateRequest = new UpdateTemplateRequest();
        updateRequest.setName("Updated Template");
        updateRequest.setType(WorkflowType.ONBOARDING);
        updateRequest.setIsActive(true);
        updateRequest.setTasks(List.of(
            createTaskRequest("Task 1", UserRole.HR_ADMIN, 1, false, null),
            createTaskRequest("Task 2", UserRole.HR_ADMIN, 1, false, null) // Duplicate, not parallel
        ));

        mockMvc.perform(put("/api/templates/{id}", created.getId())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("sequence order 1")));
    }

    // === HELPER METHODS ===

    private CreateTemplateRequest createValidTemplateRequest(String name) {
        CreateTemplateRequest request = new CreateTemplateRequest();
        request.setName(name);
        request.setDescription("Test description");
        request.setType(WorkflowType.ONBOARDING);
        request.setTasks(List.of(
            createTaskRequest("Task 1", UserRole.HR_ADMIN, 1, false, null),
            createTaskRequest("Task 2", UserRole.TECH_SUPPORT, 2, false, null)
        ));
        return request;
    }

    private CreateTemplateTaskRequest createTaskRequest(
            String taskName,
            UserRole role,
            Integer sequence,
            Boolean isParallel,
            UUID dependencyId) {
        CreateTemplateTaskRequest task = new CreateTemplateTaskRequest();
        task.setTaskName(taskName);
        task.setDescription("Task description");
        task.setAssignedRole(role);
        task.setSequenceOrder(sequence);
        task.setIsParallel(isParallel);
        task.setDependencyTaskId(dependencyId);
        return task;
    }
}
