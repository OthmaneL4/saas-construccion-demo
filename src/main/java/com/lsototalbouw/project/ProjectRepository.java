package com.lsototalbouw.project;

import com.lsototalbouw.common.enums.ProjectStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    @Query("select p from Project p join fetch p.customer where p.active = true order by p.startDate desc")
    List<Project> findByActiveTrueOrderByStartDateDesc();

    @Query("select p from Project p join fetch p.customer where p.companyAccount.id = :companyId and p.active = true order by p.startDate desc")
    List<Project> findByCompanyAccountIdAndActiveTrueOrderByStartDateDesc(@Param("companyId") Long companyId);

    @Query("select p from Project p join fetch p.customer where p.companyAccount.id = :companyId and p.customer.id = :customerId and p.active = true order by p.startDate desc")
    List<Project> findByCompanyAccountIdAndCustomerIdAndActiveTrueOrderByStartDateDesc(@Param("companyId") Long companyId,
                                                                                       @Param("customerId") Long customerId);

    @Query(value = "select p from Project p join fetch p.customer where p.companyAccount.id = :companyId and p.active = true",
            countQuery = "select count(p) from Project p where p.companyAccount.id = :companyId and p.active = true")
    Page<Project> findPageByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    @Query("select p from Project p join fetch p.customer where p.companyAccount.id = :companyId and p.active = true "
            + "and (:status is null or p.status = :status) "
            + "and (:query is null or lower(p.name) like lower(concat('%', :query, '%')) "
            + "or lower(p.customer.name) like lower(concat('%', :query, '%')) "
            + "or lower(coalesce(p.workAddress, '')) like lower(concat('%', :query, '%'))) "
            + "order by p.startDate desc")
    List<Project> searchByCompanyId(@Param("companyId") Long companyId, @Param("query") String query,
                                    @Param("status") ProjectStatus status);

    @Query(value = "select p from Project p join fetch p.customer where p.companyAccount.id = :companyId and p.active = true "
            + "and (:status is null or p.status = :status) "
            + "and (:query is null or lower(p.name) like lower(concat('%', :query, '%')) "
            + "or lower(p.customer.name) like lower(concat('%', :query, '%')) "
            + "or lower(coalesce(p.workAddress, '')) like lower(concat('%', :query, '%')))",
            countQuery = "select count(p) from Project p join p.customer where p.companyAccount.id = :companyId and p.active = true "
                    + "and (:status is null or p.status = :status) "
                    + "and (:query is null or lower(p.name) like lower(concat('%', :query, '%')) "
                    + "or lower(p.customer.name) like lower(concat('%', :query, '%')) "
                    + "or lower(coalesce(p.workAddress, '')) like lower(concat('%', :query, '%')))")
    Page<Project> searchPageByCompanyId(@Param("companyId") Long companyId, @Param("query") String query,
                                        @Param("status") ProjectStatus status, Pageable pageable);

    long countByStatusAndActiveTrue(ProjectStatus status);

    long countByCompanyAccountIdAndStatusAndActiveTrue(Long companyId, ProjectStatus status);

    @EntityGraph(attributePaths = "customer")
    Optional<Project> findByCompanyAccountIdAndIdAndActiveTrue(Long companyId, Long id);
}
