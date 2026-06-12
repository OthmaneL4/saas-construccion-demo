package com.lsototalbouw.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA Repository interface for performing database operations on {@link AppUser} entities.
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    /**
     * Finds a user by their email address, ignoring case differences.
     * Used primarily during authentication and email verification.
     *
     * @param email the email address string
     * @return an {@link Optional} containing the matched {@link AppUser}, or empty if not found
     */
    Optional<AppUser> findByEmailIgnoreCase(String email);

    /**
     * Retrieves all users registered under a specific company tenant, ordered alphabetically by their full name.
     *
     * @param companyId the unique ID of the company tenant
     * @return a list of active and inactive {@link AppUser} records
     */
    List<AppUser> findByCompanyAccountIdOrderByFullNameAsc(Long companyId);

    /**
     * Retrieves a specific user by their ID and company tenant ID.
     * Enforces tenant isolation.
     *
     * @param companyId the company tenant ID
     * @param id        the user's unique ID
     * @return an {@link Optional} containing the matched {@link AppUser}, or empty if not found
     */
    Optional<AppUser> findByCompanyAccountIdAndId(Long companyId, Long id);

    /**
     * Checks if a user already exists with the given email address (case-insensitive).
     *
     * @param email the email address to inspect
     * @return {@code true} if a user exists with the email, otherwise {@code false}
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Checks if a user already exists with the given email address (case-insensitive), excluding a specific user ID.
     * Used during profile updates to ensure email uniqueness.
     *
     * @param email the email address to inspect
     * @param id    the user ID to exclude from the check
     * @return {@code true} if a duplicate email exists on another account, otherwise {@code false}
     */
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
