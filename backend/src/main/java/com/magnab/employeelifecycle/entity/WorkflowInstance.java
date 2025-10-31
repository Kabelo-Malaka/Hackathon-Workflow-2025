package com.magnab.employeelifecycle.entity;

import com.magnab.employeelifecycle.enums.WorkflowStatus;
import com.magnab.employeelifecycle.enums.WorkflowType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * WorkflowInstance entity representing individual employee onboarding/offboarding workflows.
 * Each instance tracks a specific employee's workflow execution with status and custom field data.
 *
 * Features:
 * - UUID primary key for security and distributed systems
 * - References WorkflowTemplate (which template was used)
 * - Employee information (name, email, role)
 * - Workflow type (ONBOARDING/OFFBOARDING)
 * - Status tracking (INITIATED, IN_PROGRESS, BLOCKED, COMPLETED)
 * - Custom field values stored as JSONB for flexibility
 * - Complete audit trail with initiation and completion timestamps
 */
@Entity
@Table(name = "workflow_instances")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInstance {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "employee_name", nullable = false, length = 255)
    private String employeeName;

    @Column(name = "employee_email", nullable = false, length = 255)
    private String employeeEmail;

    @Column(name = "employee_role", nullable = false, length = 100)
    private String employeeRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "workflow_type", nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    private WorkflowType workflowType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    private WorkflowStatus status = WorkflowStatus.INITIATED;

    @Column(name = "initiated_by", nullable = false)
    private UUID initiatedBy;

    @Column(name = "initiated_at", nullable = false)
    private LocalDateTime initiatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_field_values", columnDefinition = "jsonb")
    private Map<String, Object> customFieldValues;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (initiatedAt == null) {
            initiatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
