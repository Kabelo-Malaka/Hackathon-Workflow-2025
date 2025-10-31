package com.magnab.employeelifecycle.entity;

import com.magnab.employeelifecycle.enums.WorkflowStatus;
import com.magnab.employeelifecycle.enums.WorkflowType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * WorkflowTemplate entity representing reusable workflow templates for employee lifecycle processes.
 * Each template defines a workflow type (ONBOARDING/OFFBOARDING) and contains associated tasks.
 *
 * Features:
 * - UUID primary key for security and distributed systems
 * - Workflow type enum (ONBOARDING, OFFBOARDING)
 * - Default workflow status for new workflow instances
 * - One-to-many relationship with TemplateTask
 * - Complete audit trail (created_at, created_by, updated_at, updated_by)
 */
@Entity
@Table(name = "workflow_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTemplate {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(name = "template_name", nullable = false, unique = true, length = 100)
    private String templateName;

    @Enumerated(EnumType.STRING)
    @Column(name = "workflow_type", nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    private WorkflowType workflowType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_status", nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    private WorkflowStatus defaultStatus = WorkflowStatus.INITIATED;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TemplateTask> tasks = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
