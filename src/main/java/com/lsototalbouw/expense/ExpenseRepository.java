package com.lsototalbouw.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    @Query("select e from Expense e left join fetch e.project where e.active = true order by e.expenseDate desc")
    List<Expense> findByActiveTrueOrderByExpenseDateDesc();

    @Query("select e from Expense e left join fetch e.project where e.companyAccount.id = :companyId and e.active = true order by e.expenseDate desc")
    List<Expense> findByCompanyAccountIdAndActiveTrueOrderByExpenseDateDesc(@Param("companyId") Long companyId);

    @Query("select e from Expense e left join fetch e.project where e.companyAccount.id = :companyId and e.project.id = :projectId and e.active = true order by e.expenseDate desc")
    List<Expense> findByCompanyAccountIdAndProjectIdAndActiveTrueOrderByExpenseDateDesc(@Param("companyId") Long companyId,
                                                                                        @Param("projectId") Long projectId);

    @Query("select e from Expense e left join fetch e.project where e.companyAccount.id = :companyId and e.active = true "
            + "and (:projectId is null or e.project.id = :projectId) "
            + "and (:category is null or lower(e.category) = lower(:category)) "
            + "and (:fromDate is null or e.expenseDate >= :fromDate) "
            + "and (:toDate is null or e.expenseDate <= :toDate) "
            + "and (:query is null or lower(e.description) like lower(concat('%', :query, '%')) "
            + "or lower(e.category) like lower(concat('%', :query, '%')) "
            + "or lower(coalesce(e.project.name, '')) like lower(concat('%', :query, '%'))) "
            + "order by e.expenseDate desc")
    List<Expense> searchByCompanyId(@Param("companyId") Long companyId,
                                    @Param("query") String query,
                                    @Param("projectId") Long projectId,
                                    @Param("category") String category,
                                    @Param("fromDate") LocalDate fromDate,
                                    @Param("toDate") LocalDate toDate);

    @Query("select distinct e.category from Expense e where e.companyAccount.id = :companyId and e.active = true order by e.category asc")
    List<String> findCategoriesByCompanyId(@Param("companyId") Long companyId);

    @Query("select coalesce(sum(e.amount), 0) from Expense e where e.active = true")
    BigDecimal totalExpenses();

    @Query("select coalesce(sum(e.amount), 0) from Expense e where e.companyAccount.id = :companyId and e.active = true")
    BigDecimal totalExpensesByCompanyId(@Param("companyId") Long companyId);

    @Query("select coalesce(sum(e.amount), 0) from Expense e where e.companyAccount.id = :companyId and e.project.id = :projectId and e.active = true")
    BigDecimal totalExpensesByCompanyIdAndProjectId(@Param("companyId") Long companyId, @Param("projectId") Long projectId);

    @EntityGraph(attributePaths = "project")
    Optional<Expense> findByCompanyAccountIdAndIdAndActiveTrue(Long companyId, Long id);
}
