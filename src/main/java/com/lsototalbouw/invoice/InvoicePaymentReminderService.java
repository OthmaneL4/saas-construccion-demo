package com.lsototalbouw.invoice;

import com.lsototalbouw.company.CompanyContextService;
import com.lsototalbouw.user.AppUser;
import com.lsototalbouw.user.AppUserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoicePaymentReminderService {

    private final InvoicePaymentReminderRepository reminders;
    private final AppUserRepository users;
    private final CompanyContextService companyContext;

    public InvoicePaymentReminderService(InvoicePaymentReminderRepository reminders, AppUserRepository users,
                                         CompanyContextService companyContext) {
        this.reminders = reminders;
        this.users = users;
        this.companyContext = companyContext;
    }

    @Transactional
    public InvoicePaymentReminder record(Invoice invoice, InvoicePaymentReminderType type,
                                         InvoicePaymentReminderStatus status, String message) {
        BigDecimal outstanding = invoice.getAmount().subtract(invoice.getPaidAmount());
        return reminders.save(new InvoicePaymentReminder(invoice, currentUser().orElse(null), type, status,
                outstanding, cleanMessage(message), LocalDateTime.now()));
    }

    @Transactional(readOnly = true)
    public java.util.List<InvoicePaymentReminder> listForInvoice(Long invoiceId) {
        return reminders.findByInvoiceCompanyAccountIdAndInvoiceIdAndActiveTrueOrderBySentAtDesc(
                companyContext.currentCompanyId(), invoiceId);
    }

    private Optional<AppUser> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return Optional.empty();
        }
        return users.findByEmailIgnoreCase(authentication.getName());
    }

    private String cleanMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String trimmed = message.trim();
        return trimmed.length() <= 1000 ? trimmed : trimmed.substring(0, 1000);
    }
}
