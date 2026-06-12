package com.lsototalbouw.customer;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA Repository interface for performing database operations on {@link Customer} entities.
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Retrieves all active customers globally, ordered alphabetically by name.
     *
     * @return a list of active {@link Customer} records
     */
    List<Customer> findByActiveTrueOrderByNameAsc();

    /**
     * Retrieves all active customers for a specific company tenant, ordered alphabetically by name.
     *
     * @param companyId the unique ID of the company tenant
     * @return a list of active {@link Customer} records belonging to the company
     */
    List<Customer> findByCompanyAccountIdAndActiveTrueOrderByNameAsc(Long companyId);

    /**
     * Retrieves a page of active customers for a specific company tenant.
     *
     * @param companyId the unique ID of the company tenant
     * @param pageable  the pagination information (page number, size, sorting)
     * @return a {@link Page} of active {@link Customer} records
     */
    Page<Customer> findByCompanyAccountIdAndActiveTrue(Long companyId, Pageable pageable);

    /**
     * Performs a text search over active customer attributes (name, email, phone, city) for a specific company tenant,
     * returning the matching list sorted by name.
     *
     * @param companyId the unique ID of the company tenant
     * @param query     the search term query string
     * @return a list of matching active {@link Customer} records
     */
    @Query("select c from Customer c where c.companyAccount.id = :companyId and c.active = true "
            + "and (lower(c.name) like lower(concat('%', :query, '%')) "
            + "or lower(coalesce(c.email, '')) like lower(concat('%', :query, '%')) "
            + "or lower(coalesce(c.phone, '')) like lower(concat('%', :query, '%')) "
            + "or lower(coalesce(c.city, '')) like lower(concat('%', :query, '%'))) "
            + "order by c.name asc")
    List<Customer> searchByCompanyId(@Param("companyId") Long companyId, @Param("query") String query);

    /**
     * Performs a paginated text search over active customer attributes (name, email, phone, city)
     * for a specific company tenant.
     *
     * @param companyId the unique ID of the company tenant
     * @param query     the search term query string
     * @param pageable  the pagination information
     * @return a {@link Page} of matching active {@link Customer} records
     */
    @Query("select c from Customer c where c.companyAccount.id = :companyId and c.active = true "
            + "and (lower(c.name) like lower(concat('%', :query, '%')) "
            + "or lower(coalesce(c.email, '')) like lower(concat('%', :query, '%')) "
            + "or lower(coalesce(c.phone, '')) like lower(concat('%', :query, '%')) "
            + "or lower(coalesce(c.city, '')) like lower(concat('%', :query, '%')))")
    Page<Customer> searchByCompanyId(@Param("companyId") Long companyId, @Param("query") String query, Pageable pageable);

    /**
     * Counts the total number of active customers registered under a company tenant.
     *
     * @param companyId the unique ID of the company tenant
     * @return the total count of active customers
     */
    long countByCompanyAccountIdAndActiveTrue(Long companyId);

    /**
     * Finds an active customer by their unique ID and company tenant ID.
     * Ensures tenant isolation checks during customer loading.
     *
     * @param companyId the company tenant ID
     * @param id        the customer's unique ID
     * @return an {@link Optional} containing the matched {@link Customer}, or empty if not found or archived
     */
    Optional<Customer> findByCompanyAccountIdAndIdAndActiveTrue(Long companyId, Long id);
}
