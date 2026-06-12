package com.lsototalbouw.tool;

import com.lsototalbouw.common.enums.ToolStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ToolRepository extends JpaRepository<ToolItem, Long> {
    List<ToolItem> findByActiveTrueOrderByNameAsc();

    List<ToolItem> findByCompanyAccountIdAndActiveTrueOrderByNameAsc(Long companyId);

    @Query("""
            select tool
            from ToolItem tool
            where tool.companyAccount.id = :companyId
              and tool.active = true
              and (:status is null or tool.status = :status)
              and (:maintenanceDue = false or tool.nextMaintenanceDate <= :today)
              and (
                    :query is null
                 or lower(tool.name) like lower(concat('%', :query, '%'))
                 or lower(coalesce(tool.serialNumber, '')) like lower(concat('%', :query, '%'))
              )
            order by tool.name asc
            """)
    List<ToolItem> searchByCompanyId(@Param("companyId") Long companyId,
                                      @Param("query") String query,
                                      @Param("status") ToolStatus status,
                                      @Param("maintenanceDue") boolean maintenanceDue,
                                      @Param("today") LocalDate today);

    Optional<ToolItem> findByCompanyAccountIdAndIdAndActiveTrue(Long companyId, Long id);

    long countByStatusAndActiveTrue(ToolStatus status);

    long countByCompanyAccountIdAndStatusAndActiveTrue(Long companyId, ToolStatus status);
}
