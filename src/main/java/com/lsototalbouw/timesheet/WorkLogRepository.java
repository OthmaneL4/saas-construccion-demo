package com.lsototalbouw.timesheet;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkLogRepository extends JpaRepository<WorkLog, Long> {

    @EntityGraph(attributePaths = {"project", "project.customer", "invoiceLine", "invoiceLine.invoice"})
    List<WorkLog> findByCompanyAccountIdAndActiveTrueOrderByWorkDateDescCreatedAtDesc(Long companyId);

    @EntityGraph(attributePaths = {"project", "project.customer", "invoiceLine", "invoiceLine.invoice"})
    List<WorkLog> findByCompanyAccountIdAndProjectIdAndActiveTrueOrderByWorkDateDescCreatedAtDesc(Long companyId,
                                                                                                  Long projectId);

    @EntityGraph(attributePaths = {"project", "project.customer", "invoiceLine", "invoiceLine.invoice"})
    @Query("""
            select workLog
            from WorkLog workLog
            join workLog.project project
            join project.customer customer
            where workLog.companyAccount.id = :companyId
              and workLog.active = true
              and (:projectId is null or project.id = :projectId)
              and (:status is null or workLog.status = :status)
              and (:billable is null or workLog.billable = :billable)
              and (
                    :query is null
                 or lower(workLog.workerName) like lower(concat('%', :query, '%'))
                 or lower(workLog.description) like lower(concat('%', :query, '%'))
                 or lower(project.name) like lower(concat('%', :query, '%'))
                 or lower(customer.name) like lower(concat('%', :query, '%'))
              )
            order by workLog.workDate desc, workLog.createdAt desc
            """)
    List<WorkLog> searchByCompanyId(@Param("companyId") Long companyId,
                                    @Param("projectId") Long projectId,
                                    @Param("status") WorkLogStatus status,
                                    @Param("billable") Boolean billable,
                                    @Param("query") String query);

    @EntityGraph(attributePaths = {"project", "project.customer", "invoiceLine", "invoiceLine.invoice"})
    Optional<WorkLog> findByCompanyAccountIdAndIdAndActiveTrue(Long companyId, Long id);

    @Query("select coalesce(sum(w.hours), 0) from WorkLog w where w.companyAccount.id = :companyId and w.project.id = :projectId and w.active = true")
    java.math.BigDecimal totalHoursByCompanyIdAndProjectId(@Param("companyId") Long companyId, @Param("projectId") Long projectId);

    @Query("select coalesce(sum(w.hours * w.hourlyRate), 0) from WorkLog w where w.companyAccount.id = :companyId and w.project.id = :projectId and w.active = true")
    java.math.BigDecimal totalLaborValueByCompanyIdAndProjectId(@Param("companyId") Long companyId, @Param("projectId") Long projectId);
}
