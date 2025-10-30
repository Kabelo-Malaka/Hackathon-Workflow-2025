package com.magnab.employeelifecycle.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnab.employeelifecycle.dto.request.CreateUserRequest;
import com.magnab.employeelifecycle.enums.UserRole;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UserControllerIntegrationTest {

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

    @BeforeEach
    void setUp() {
        // Test setup if needed
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMINISTRATOR"})
    void createUser_WithValidData_Returns201() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser" + System.currentTimeMillis());
        request.setEmail("test" + System.currentTimeMillis() + "@magnab.com");
        request.setPassword("password123");
        request.setRole(UserRole.HR_ADMIN);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").exists())
            .andExpect(jsonPath("$.role").value("HR_ADMIN"))
            .andExpect(jsonPath("$.isActive").value(true))
            .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMINISTRATOR"})
    void getUsers_ReturnsUserList() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }


    @Test
    void createUser_WithoutAuthentication_Returns401() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setEmail("test@magnab.com");
        request.setPassword("password123");
        request.setRole(UserRole.HR_ADMIN);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "linemanager", roles = {"LINE_MANAGER"})
    void createUser_WithoutHRAdminRole_Returns403() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setEmail("test@magnab.com");
        request.setPassword("password123");
        request.setRole(UserRole.HR_ADMIN);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }
}
