package com.ikaansh.dhananjay.TaskModule.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Task owned by exactly one {@link User}.
 *
 * Index notes (mirrors db/migration/V2__create_tasks_table.sql):
 *  - idx_tasks_owner_status_due : owner listing, owner + status filter, due-date sorting
 *  - idx_tasks_owner_created    : newest-first listing and duplicate-submit check
 *  - idx_tasks_status_due       : ADMIN listing filtered by status
 */
@Entity
@Table(
        name = "tasks",
        indexes = {
                @Index(name = "idx_tasks_owner_status_due", columnList = "owner_id, status, due_date"),
                @Index(name = "idx_tasks_owner_created", columnList = "owner_id, created_at"),
                @Index(name = "idx_tasks_status_due", columnList = "status, due_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskStatus status;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    // LAZY on purpose: list queries use @EntityGraph so there is no N+1
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Optimistic lock - last safety net behind the pessimistic row lock
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
