package com.lsototalbouw.invoice;

import com.lsototalbouw.common.entity.BaseEntity;
import com.lsototalbouw.company.CompanyAccount;
import com.lsototalbouw.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_payment_reminders", indexes = {
        @Index(name = "idx_invoice_payment_reminders_invoice", columnList = "invoice_id, sent_at"),
        @Index(name = "idx_invoice_payment_reminders_company", columnList = "company_account_id, sent_at")
})
public class InvoicePaymentReminder extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_account_id", nullable = false)
    private CompanyAccount companyAccount;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", nullable = false, length = 40)
    private InvoicePaymentReminderType reminderType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private InvoicePaymentReminderStatus status;

    @Column(name = "outstanding_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal outstandingAmount;

    @Column(length = 1000)
    private String message;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "generated_by_name", length = 120)
    private String generatedByName;

    @Column(name = "generated_by_email", length = 160)
    private String generatedByEmail;

    protected InvoicePaymentReminder() {
    }

    public InvoicePaymentReminder(Invoice invoice, AppUser user, InvoicePaymentReminderType reminderType,
                                  InvoicePaymentReminderStatus status, BigDecimal outstandingAmount,
                                  String message, LocalDateTime sentAt) {
        this.companyAccount = invoice.getCompanyAccount();
        this.invoice = invoice;
        this.user = user;
        this.reminderType = reminderType;
        this.status = status;
        this.outstandingAmount = outstandingAmount;
        this.message = message;
        this.sentAt = sentAt;
        if (user != null) {
            this.generatedByName = user.getFullName();
            this.generatedByEmail = user.getEmail();
        }
    }

    public InvoicePaymentReminderType getReminderType() {
        return reminderType;
    }

    public InvoicePaymentReminderStatus getStatus() {
        return status;
    }

    public BigDecimal getOutstandingAmount() {
        return outstandingAmount;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public String getGeneratedByName() {
        return generatedByName;
    }

    public String getGeneratedByEmail() {
        return generatedByEmail;
    }
}
