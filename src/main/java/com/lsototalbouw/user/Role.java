package com.lsototalbouw.user;

import com.lsototalbouw.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Domain entity representing an authorization role in the application.
 *
 * <p>Roles govern user access controls and permission scopes for the controllers and endpoints.
 * Extension of {@link BaseEntity}.
 */
@Entity
@Table(name = "roles")
public class Role extends BaseEntity {

    /** The unique security identifier of the role (e.g. {@code "ROLE_ADMIN", "ROLE_FINANCE"}). */
    @Column(nullable = false, unique = true, length = 60)
    private String name;

    /**
     * Protected no-arg constructor required by JPA.
     */
    protected Role() {
    }

    /**
     * Initializes a new role with its unique system authority name.
     *
     * @param name the unique role name
     */
    public Role(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
