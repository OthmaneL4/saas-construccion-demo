package com.lsototalbouw.notification;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BusinessNotificationRepository extends JpaRepository<BusinessNotification, Long> {

    @Query("select n from BusinessNotification n where n.companyAccount.id = :companyId and n.active = true "
            + "order by case when n.readAt is null then 0 else 1 end, "
            + "case when n.dueDate is null then 1 else 0 end, n.dueDate asc, n.createdAt desc")
    List<BusinessNotification> findCurrentCompanyNotifications(@Param("companyId") Long companyId);

    Optional<BusinessNotification> findByCompanyAccountIdAndSourceKeyAndActiveTrue(Long companyId, String sourceKey);

    Optional<BusinessNotification> findByCompanyAccountIdAndIdAndActiveTrue(Long companyId, Long id);

    long countByCompanyAccountIdAndReadAtIsNullAndActiveTrue(Long companyId);

    List<BusinessNotification> findByCompanyAccountIdAndReadAtIsNullAndActiveTrue(Long companyId);
}
