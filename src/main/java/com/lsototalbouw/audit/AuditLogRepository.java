package com.lsototalbouw.audit;

import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @EntityGraph(attributePaths = "user")
    List<AuditLog> findTop100ByCompanyAccountIdAndActiveTrueOrderByCreatedAtDesc(Long companyId);

    @EntityGraph(attributePaths = "user")
    @Query("""
            select log
            from AuditLog log
            left join log.user user
            where log.companyAccount.id = :companyId
              and log.active = true
              and (:action is null or log.action = :action)
              and (:moduleName is null or lower(log.moduleName) like lower(concat('%', :moduleName, '%')))
              and (:userEmail is null or lower(user.email) like lower(concat('%', :userEmail, '%')))
              and (:fromDate is null or log.createdAt >= :fromDate)
              and (:toDate is null or log.createdAt <= :toDate)
            order by log.createdAt desc
            """)
    List<AuditLog> searchByCompanyId(@Param("companyId") Long companyId,
                                     @Param("action") AuditAction action,
                                     @Param("moduleName") String moduleName,
                                     @Param("userEmail") String userEmail,
                                     @Param("fromDate") LocalDateTime fromDate,
                                     @Param("toDate") LocalDateTime toDate,
                                     Pageable pageable);

    @Query("""
            select distinct log.moduleName
            from AuditLog log
            where log.companyAccount.id = :companyId
              and log.active = true
            order by log.moduleName asc
            """)
    List<String> findDistinctModuleNamesByCompanyId(@Param("companyId") Long companyId);
}
