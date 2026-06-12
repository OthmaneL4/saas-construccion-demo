package com.lsototalbouw.payment;

import com.lsototalbouw.audit.AuditAction;
import com.lsototalbouw.audit.AuditService;
import com.lsototalbouw.common.enums.PaymentStatus;
import com.lsototalbouw.common.web.SafeRedirects;
import com.lsototalbouw.company.CompanyContextService;
import com.lsototalbouw.invoice.Invoice;
import com.lsototalbouw.invoice.InvoiceRepository;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class PaymentController {

    private final PaymentRepository payments;
    private final PaymentService paymentService;
    private final InvoiceRepository invoices;
    private final CompanyContextService companyContext;
    private final AuditService auditService;

    public PaymentController(PaymentRepository payments, PaymentService paymentService,
                             InvoiceRepository invoices, CompanyContextService companyContext,
                             AuditService auditService) {
        this.payments = payments;
        this.paymentService = paymentService;
        this.invoices = invoices;
        this.companyContext = companyContext;
        this.auditService = auditService;
    }

    @GetMapping("/payments")
    public String index(Model model, @ModelAttribute("paymentForm") PaymentForm paymentForm,
                        @RequestParam(name = "invoiceId", required = false) Long invoiceId,
                        @RequestParam(name = "status", required = false) PaymentStatus status,
                        @RequestParam(name = "q", required = false) String query,
                        @RequestParam(name = "from", required = false) LocalDate fromDate,
                        @RequestParam(name = "to", required = false) LocalDate toDate,
                        @RequestParam(name = "returnUrl", required = false) String returnUrl) {
        prepareModel(model, invoiceId, status, query, fromDate, toDate);
        model.addAttribute("paymentForm", preparedPaymentForm(paymentForm, invoiceId, returnUrl));
        return "payments/index";
    }

    @PostMapping("/payments")
    public String create(@Valid @ModelAttribute("paymentForm") PaymentForm form,
                         BindingResult bindingResult,
                         Model model,
                          RedirectAttributes redirectAttributes) {
        paymentService.validate(form, bindingResult);
        if (bindingResult.hasErrors()) {
            prepareModel(model, form.getInvoiceId(), null, null, null, null);
            return "payments/index";
        }
        Payment payment = paymentService.create(form);
        auditService.record(AuditAction.CREATE, "Pagos", payment.getId(),
                "Pago registrado: " + payment.getInvoice().getInvoiceNumber());
        redirectAttributes.addFlashAttribute("successMessage", "Pago registrado correctamente.");
        return SafeRedirects.redirectTo(form.getReturnUrl(), "/payments");
    }

    @GetMapping("/payments/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "Pago");
        model.addAttribute("payment", paymentService.getCurrentCompanyPayment(id));
        return "payments/detail";
    }

    @GetMapping("/payments/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        Payment payment = paymentService.getCurrentCompanyPayment(id);
        prepareModel(model, null, payment);
        model.addAttribute("pageTitle", "Editar pago");
        model.addAttribute("payment", payment);
        model.addAttribute("paymentForm", PaymentForm.from(payment));
        return "payments/edit";
    }

    @PostMapping("/payments/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("paymentForm") PaymentForm form,
                         BindingResult bindingResult,
                         Model model,
                          RedirectAttributes redirectAttributes) {
        paymentService.validate(id, form, bindingResult);
        if (bindingResult.hasErrors()) {
            Payment payment = paymentService.getCurrentCompanyPayment(id);
            prepareModel(model, form.getInvoiceId(), payment);
            model.addAttribute("pageTitle", "Editar pago");
            model.addAttribute("payment", payment);
            return "payments/edit";
        }
        Payment payment = paymentService.update(id, form);
        auditService.record(AuditAction.UPDATE, "Pagos", id,
                "Pago actualizado: " + payment.getInvoice().getInvoiceNumber());
        redirectAttributes.addFlashAttribute("successMessage", "Pago actualizado correctamente.");
        return "redirect:/payments/{id}";
    }

    @PostMapping("/payments/{id}/confirm")
    public String confirm(@PathVariable Long id,
                          @RequestParam(name = "returnUrl", required = false) String returnUrl,
                          RedirectAttributes redirectAttributes) {
        try {
            Payment payment = paymentService.confirm(id);
            auditService.record(AuditAction.UPDATE, "Pagos", id,
                    "Pago confirmado: " + payment.getInvoice().getInvoiceNumber());
            redirectAttributes.addFlashAttribute("successMessage", "Pago confirmado correctamente.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return SafeRedirects.redirectTo(returnUrl, "/payments/" + id);
    }

    @PostMapping("/payments/{id}/archive")
    public String archive(@PathVariable Long id,
                          @RequestParam(name = "returnUrl", required = false) String returnUrl,
                          RedirectAttributes redirectAttributes) {
        paymentService.archive(id);
        auditService.record(AuditAction.ARCHIVE, "Pagos", id, "Pago archivado");
        redirectAttributes.addFlashAttribute("successMessage", "Pago archivado correctamente.");
        return SafeRedirects.redirectTo(returnUrl, "/payments");
    }

    private void prepareModel(Model model, Long invoiceId) {
        prepareModel(model, invoiceId, null, null, null, null, null);
    }

    private void prepareModel(Model model, Long invoiceId, PaymentStatus status, String query) {
        prepareModel(model, invoiceId, status, query, null, null, null);
    }

    private void prepareModel(Model model, Long invoiceId, PaymentStatus status, String query,
                              LocalDate fromDate, LocalDate toDate) {
        prepareModel(model, invoiceId, status, query, fromDate, toDate, null);
    }

    private void prepareModel(Model model, Long invoiceId, Payment paymentContext) {
        prepareModel(model, invoiceId, null, null, null, null, paymentContext);
    }

    private void prepareModel(Model model, Long invoiceId, PaymentStatus status, String query,
                              LocalDate fromDate, LocalDate toDate, Payment paymentContext) {
        Long companyId = companyContext.currentCompanyId();
        Long selectedInvoiceId = validInvoiceId(companyId, invoiceId);
        String cleanQuery = query == null || query.isBlank() ? null : query.trim();
        List<Invoice> companyInvoices = invoices.findByCompanyAccountIdAndActiveTrueOrderByDueDateAsc(companyId);
        boolean hasFilters = selectedInvoiceId != null || status != null || cleanQuery != null
                || fromDate != null || toDate != null;
        List<Payment> filteredPayments = !hasFilters
                ? payments.findByCompanyAccountIdAndActiveTrueOrderByPaymentDateDesc(companyId)
                : payments.searchByCompanyId(companyId, selectedInvoiceId, status, cleanQuery, fromDate, toDate);
        model.addAttribute("pageTitle", "Pagos");
        model.addAttribute("payments", filteredPayments);
        model.addAttribute("summary", PaymentSummary.from(filteredPayments));
        model.addAttribute("invoices", companyInvoices);
        model.addAttribute("invoiceAvailableAmounts", invoiceAvailableAmounts(companyInvoices, paymentContext));
        model.addAttribute("statuses", PaymentStatus.values());
        model.addAttribute("selectedInvoiceId", selectedInvoiceId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("query", cleanQuery);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("paymentsReturnUrl", paymentsReturnUrl(selectedInvoiceId, status, cleanQuery, fromDate, toDate));
    }

    private Map<Long, BigDecimal> invoiceAvailableAmounts(List<Invoice> companyInvoices, Payment paymentContext) {
        Map<Long, BigDecimal> availableAmounts = new LinkedHashMap<>();
        for (Invoice invoice : companyInvoices) {
            BigDecimal availableAmount = invoice.getOutstandingAmount();
            if (paymentContext != null
                    && paymentContext.getStatus() == PaymentStatus.COMPLETED
                    && paymentContext.getInvoice().getId().equals(invoice.getId())) {
                availableAmount = availableAmount.add(paymentContext.getAmount());
            }
            availableAmounts.put(invoice.getId(), availableAmount);
        }
        return availableAmounts;
    }

    private PaymentForm preparedPaymentForm(PaymentForm incomingForm, Long invoiceId, String returnUrl) {
        if (incomingForm != null && !incomingForm.isEmptyNewForm()) {
            incomingForm.setReturnUrl(safeReturnUrl(incomingForm.getReturnUrl(), returnUrl));
            return incomingForm;
        }

        PaymentForm form = new PaymentForm();
        Long companyId = companyContext.currentCompanyId();
        validInvoice(companyId, invoiceId).ifPresent(invoice -> {
            form.setInvoiceId(invoice.getId());
            form.setAmount(invoice.getOutstandingAmount());
        });
        if (SafeRedirects.isLocalPath(returnUrl)) {
            form.setReturnUrl(returnUrl);
        }
        return form;
    }

    private String safeReturnUrl(String formReturnUrl, String requestReturnUrl) {
        if (SafeRedirects.isLocalPath(formReturnUrl)) {
            return formReturnUrl;
        }
        if (SafeRedirects.isLocalPath(requestReturnUrl)) {
            return requestReturnUrl;
        }
        return null;
    }

    private Long validInvoiceId(Long companyId, Long invoiceId) {
        return validInvoice(companyId, invoiceId)
                .map(Invoice::getId)
                .orElse(null);
    }

    private java.util.Optional<Invoice> validInvoice(Long companyId, Long invoiceId) {
        if (invoiceId == null) {
            return java.util.Optional.empty();
        }
        return invoices.findByCompanyAccountIdAndIdAndActiveTrue(companyId, invoiceId);
    }

    private String paymentsReturnUrl(Long invoiceId, PaymentStatus status, String query, LocalDate fromDate, LocalDate toDate) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/payments");
        if (invoiceId != null) {
            builder.queryParam("invoiceId", invoiceId);
        }
        if (status != null) {
            builder.queryParam("status", status);
        }
        if (query != null && !query.isBlank()) {
            builder.queryParam("q", query);
        }
        if (fromDate != null) {
            builder.queryParam("from", fromDate);
        }
        if (toDate != null) {
            builder.queryParam("to", toDate);
        }
        return builder.build().toUriString();
    }

}
