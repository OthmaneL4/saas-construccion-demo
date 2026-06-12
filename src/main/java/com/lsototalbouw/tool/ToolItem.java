package com.lsototalbouw.tool;

import com.lsototalbouw.common.entity.BaseEntity;
import com.lsototalbouw.common.enums.ToolStatus;
import com.lsototalbouw.company.CompanyAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "tools", indexes = {
        @Index(name = "idx_tools_company_active_name", columnList = "company_account_id, active, name"),
        @Index(name = "idx_tools_company_status_active", columnList = "company_account_id, status, active")
})
public class ToolItem extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_account_id", nullable = false)
    private CompanyAccount companyAccount;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 80)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ToolStatus status;

    private LocalDate nextMaintenanceDate;

    protected ToolItem() {
    }

    public ToolItem(CompanyAccount companyAccount, String name, String serialNumber,
                    ToolStatus status, LocalDate nextMaintenanceDate) {
        this.companyAccount = companyAccount;
        this.name = name;
        this.serialNumber = serialNumber;
        this.status = status;
        this.nextMaintenanceDate = nextMaintenanceDate;
    }

    public String getName() {
        return name;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public ToolStatus getStatus() {
        return status;
    }

    public LocalDate getNextMaintenanceDate() {
        return nextMaintenanceDate;
    }

    public void updateFrom(String name, String serialNumber, ToolStatus status, LocalDate nextMaintenanceDate) {
        this.name = name;
        this.serialNumber = serialNumber;
        this.status = status;
        this.nextMaintenanceDate = nextMaintenanceDate;
    }
}
