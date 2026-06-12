package com.lsototalbouw.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.EntityListeners;

/**
 * Abstract base class for all persistent entities in the application.
 *
 * <p>Provides automatic tracking of entity lifecycle events, including creation time,
 * modification time, and support for soft deletion using the {@code active} status flag.
 *
 * <p>This class uses Spring Data JPA's {@link AuditingEntityListener} to automatically
 * populate audit timestamps on persist and update operations.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /**
     * The primary key identifier of the entity.
     * Automatically generated using the database identity column strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The timestamp indicating when the entity was first persisted to the database.
     * Automatically populated and immutable after creation.
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * The timestamp indicating when the entity was last updated in the database.
     * Automatically updated on every save/update operation.
     */
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * A flag representing the soft-deletion status of the entity.
     * If {@code true}, the entity is active and visible in the application.
     * If {@code false}, the entity has been archived or soft-deleted.
     */
    @Column(nullable = false)
    private boolean active = true;

    /**
     * Retrieves the primary key identifier.
     *
     * @return the database-generated ID, or {@code null} if the entity is not yet persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Retrieves the creation timestamp of the entity.
     *
     * @return the {@link LocalDateTime} of persistence, or {@code null} if not audited yet
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Retrieves the last modified timestamp of the entity.
     *
     * @return the last updated {@link LocalDateTime}, or {@code null} if not audited yet
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Checks if the entity is active (not soft-deleted).
     *
     * @return {@code true} if active, {@code false} if archived/soft-deleted
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets the active status flag of the entity. Used primarily for archiving (soft deleting).
     *
     * @param active the new active status flag
     */
    public void setActive(boolean active) {
        this.active = active;
    }
}
