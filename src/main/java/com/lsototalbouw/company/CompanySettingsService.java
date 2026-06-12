package com.lsototalbouw.company;

import com.lsototalbouw.audit.AuditAction;
import com.lsototalbouw.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class handling tenant configuration and profile metadata modifications.
 *
 * <p>Enforces transactional rules for profile updates, handles string validation
 * (trimming, IBAN normalization), and logs audit events to track system configurations changes.
 */
@Service
public class CompanySettingsService {

    private final CompanyContextService companyContext;
    private final AuditService auditService;

    public CompanySettingsService(CompanyContextService companyContext, AuditService auditService) {
        this.companyContext = companyContext;
        this.auditService = auditService;
    }

    /**
     * Resolves and extracts the current tenant's profile parameters as a web form object.
     *
     * @return a {@link CompanySettingsForm} containing the current company profile data
     */
    @Transactional(readOnly = true)
    public CompanySettingsForm currentSettings() {
        return CompanySettingsForm.from(companyContext.currentCompany());
    }

    /**
     * Updates the current company's profile information based on form inputs.
     *
     * <p>This method updates the current tenant's database entry and registers
     * an audit log tracing the update action.
     *
     * @param form the settings form containing updated profile fields
     * @return the updated and managed {@link CompanyAccount} entity
     * @throws IllegalStateException if no current company context can be resolved
     */
    @Transactional
    public CompanyAccount update(CompanySettingsForm form) {
        CompanyAccount company = companyContext.currentCompany();
        company.updateFrom(
                trim(form.getName()),
                trim(form.getVatNumber()),
                trim(form.getEmail()),
                trim(form.getPhone()),
                trim(form.getAddress()),
                trim(form.getKvkNumber()),
                normalizeIban(form.getIban()),
                form.getPaymentTermsDays()
        );
        auditService.record(AuditAction.UPDATE, "COMPANY_SETTINGS", company.getId(),
                "Datos de empresa actualizados", company.getName());
        return company;
    }

    /**
     * Helper utility to trim whitespace from text inputs safely.
     *
     * @param value the string to trim
     * @return the trimmed string, or {@code null} if the input was null
     */
    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * Normalizes International Bank Account Numbers (IBAN) for clean database storage.
     *
     * <p>Removes all embedded spaces and converts the string to uppercase.
     *
     * @param value the raw IBAN input string
     * @return the normalized IBAN string, or {@code null} if the input was null
     */
    private String normalizeIban(String value) {
        return value == null ? null : value.replace(" ", "").trim().toUpperCase();
    }
}
