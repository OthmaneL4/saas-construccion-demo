package com.lsototalbouw.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA Repository interface for performing database operations on {@link Role} entities.
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Retrieves a role by its unique authority name.
     *
     * @param name the authority name of the role (e.g. {@code "ROLE_ADMIN"})
     * @return an {@link Optional} containing the matched {@link Role}, or empty if not found
     */
    Optional<Role> findByName(String name);
}
