package com.magnab.employeelifecycle.entity;

import com.magnab.employeelifecycle.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * TemplateTask entity representing individual tasks within a workflow template.
 * Tasks are ordered by sequence_order and can have dependencies on other tasks.
 *
 * Features:
 * - UUID primary key for security and distributed systems
 * - Many-to-one relationship with WorkflowTemplate
 * - Self-referential relationship for task dependencies
 * - Sequence order for task execution ordering
 * - Complete audit trail (created_at, created_by, updated_at, updated_by)
 */
@Entity
@Table(name = "template_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateTask {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(name = "template_id", nullable = false, insertable = false, updatable = false)
    private UUID templateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private WorkflowTemplate template;

    @Column(name = "task_name", nullable = false, length = 100)
    private String taskName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_role", nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    private UserRole assignedRole = UserRole.HR_ADMIN;

    @Column(name = "is_parallel", nullable = false)
    private Boolean isParallel = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depends_on_task_id")
    private TemplateTask dependsOnTask;

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
