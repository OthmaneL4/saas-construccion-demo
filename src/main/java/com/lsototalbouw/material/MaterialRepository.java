package com.lsototalbouw.material;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaterialRepository extends JpaRepository<MaterialItem, Long> {
    List<MaterialItem> findByActiveTrueOrderByNameAsc();

    List<MaterialItem> findByCompanyAccountIdAndActiveTrueOrderByNameAsc(Long companyId);

    @Query("select m from MaterialItem m where m.companyAccount.id = :companyId and m.active = true "
            + "and (:lowStockOnly = false or m.stockQuantity <= m.minimumStock) "
            + "and (:query is null or lower(m.name) like lower(concat('%', :query, '%')) "
            + "or lower(m.unit) like lower(concat('%', :query, '%'))) "
            + "order by m.name asc")
    List<MaterialItem> searchByCompanyId(@Param("companyId") Long companyId,
                                         @Param("query") String query,
                                         @Param("lowStockOnly") boolean lowStockOnly);

    Optional<MaterialItem> findByCompanyAccountIdAndIdAndActiveTrue(Long companyId, Long id);

    long countByStockQuantityLessThanEqualAndActiveTrue(int stockQuantity);

    @Query("select count(m) from MaterialItem m where m.companyAccount.id = :companyId and m.active = true and m.stockQuantity <= m.minimumStock")
    long countLowStockByCompanyId(@Param("companyId") Long companyId);
}
