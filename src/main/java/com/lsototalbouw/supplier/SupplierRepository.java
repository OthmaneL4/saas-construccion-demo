package com.lsototalbouw.supplier;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findByCompanyAccountIdAndActiveTrueOrderByNameAsc(Long companyId);

    Page<Supplier> findByCompanyAccountIdAndActiveTrue(Long companyId, Pageable pageable);

    long countByCompanyAccountIdAndActiveTrue(Long companyId);

    @Query("""
            select count(supplier)
            from Supplier supplier
            where supplier.companyAccount.id = :companyId
              and supplier.active = true
              and supplier.email is not null
              and supplier.email <> ''
            """)
    long countWithEmailByCompanyId(@Param("companyId") Long companyId);

    @Query("""
            select count(supplier)
            from Supplier supplier
            where supplier.companyAccount.id = :companyId
              and supplier.active = true
              and supplier.phone is not null
              and supplier.phone <> ''
            """)
    long countWithPhoneByCompanyId(@Param("companyId") Long companyId);

    @Query("""
            select count(distinct supplier.city)
            from Supplier supplier
            where supplier.companyAccount.id = :companyId
              and supplier.active = true
              and supplier.city is not null
              and supplier.city <> ''
            """)
    long countCoveredCitiesByCompanyId(@Param("companyId") Long companyId);

    @Query("""
            select supplier
            from Supplier supplier
            where supplier.companyAccount.id = :companyId
              and supplier.active = true
              and (
                    lower(supplier.name) like lower(concat('%', :query, '%'))
                 or lower(coalesce(supplier.contactName, '')) like lower(concat('%', :query, '%'))
                 or lower(coalesce(supplier.email, '')) like lower(concat('%', :query, '%'))
                 or lower(coalesce(supplier.phone, '')) like lower(concat('%', :query, '%'))
                 or lower(coalesce(supplier.city, '')) like lower(concat('%', :query, '%'))
              )
            """)
    Page<Supplier> searchByCompanyId(@Param("companyId") Long companyId,
                                     @Param("query") String query,
                                     Pageable pageable);

    Optional<Supplier> findByCompanyAccountIdAndIdAndActiveTrue(Long companyId, Long id);
}
