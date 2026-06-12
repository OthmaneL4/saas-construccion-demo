package com.lsototalbouw.payment;

import com.lsototalbouw.common.enums.PaymentStatus;
import com.lsototalbouw.company.CompanyContextService;
import com.lsototalbouw.invoice.Invoice;
import com.lsototalbouw.invoice.InvoiceRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

/**
 * Coordinates payment lifecycle operations and keeps invoice balances consistent.
 *
 * <p>Payments may be recorded as pending commitments or completed receipts. Completed payments immediately
 * adjust the linked invoice paid amount, while updates and archives reverse previous completed amounts before
 * applying the new state. All reads and writes are scoped to the current company.
 */
@Service
public class PaymentService {

    private final PaymentRepository payments;
    private final InvoiceRepository invoices;
    private final CompanyContextService companyContext;

    public PaymentService(PaymentRepository payments, InvoiceRepository invoices, CompanyContextService companyContext) {
        this.payments = payments;
        this.invoices = invoices;
        this.companyContext = companyContext;
    }

    /**
     * Loads an active payment that belongs to the current company.
     *
     * @param id payment identifier from the route
     * @return the tenant-scoped payment
     * @throws IllegalArgumentException when the payment does not belong to the current company or is archived
     */
    @Transactional(readOnly = true)
    public Payment getCurrentCompanyPayment(Long id) {
        return payments.findByCompanyAccountIdAndIdAndActiveTrue(companyContext.currentCompanyId(), id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));
    }

    /**
     * Validates a new payment against its linked invoice before persistence.
     *
     * @param form          submitted payment form
     * @param bindingResult validation sink used by the MVC controller
     */
    @Transactional(readOnly = true)
    public void validate(PaymentForm form, BindingResult bindingResult) {
        validate(null, form, bindingResult);
    }

    /**
     * Validates a payment create or update request against the invoice outstanding amount.
     *
     * <p>When editing an already completed payment, the existing amount is temporarily added back to the
     * available balance so the user can adjust the payment without triggering a false overpayment error.
     *
     * @param paymentId     optional existing payment identifier
     * @param form          submitted payment form
     * @param bindingResult validation sink used by the MVC controller
     */
    @Transactional(readOnly = true)
    public void validate(Long paymentId, PaymentForm form, BindingResult bindingResult) {
        if (form.getInvoiceId() == null || form.getAmount() == null || form.getStatus() == null) {
            return;
        }
        Long companyId = companyContext.currentCompanyId();
        Invoice invoice = invoices.findByCompanyAccountIdAndIdAndActiveTrue(companyId, form.getInvoiceId())
                .orElse(null);
        if (invoice == null) {
            bindingResult.rejectValue("invoiceId", "payment.invoice.notFound", "Factura no encontrada");
            return;
        }
        if (form.getStatus() != PaymentStatus.COMPLETED) {
            return;
        }

        BigDecimal availableAmount = invoice.getOutstandingAmount();
        if (paymentId != null) {
            Payment existingPayment = payments.findByCompanyAccountIdAndIdAndActiveTrue(companyId, paymentId)
                    .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));
            if (existingPayment.getStatus() == PaymentStatus.COMPLETED
                    && existingPayment.getInvoice().getId().equals(invoice.getId())) {
                availableAmount = availableAmount.add(existingPayment.getAmount());
            }
        }

        if (form.getAmount().compareTo(availableAmount) > 0) {
            bindingResult.rejectValue("amount", "payment.amount.exceedsOutstanding",
                    "El importe no puede superar el pendiente de la factura");
        }
    }

    /**
     * Creates a payment for the selected invoice and updates the invoice balance when it is completed.
     *
     * @param form validated payment form
     * @return the persisted payment
     */
    @Transactional
    public Payment create(PaymentForm form) {
        Long companyId = companyContext.currentCompanyId();
        Invoice invoice = invoices.findByCompanyAccountIdAndIdAndActiveTrue(companyId, form.getInvoiceId())
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada"));
        Payment payment = new Payment(
                invoice.getCompanyAccount(),
                invoice,
                form.getAmount(),
                form.getPaymentDate(),
                form.getMethod().trim(),
                form.getStatus()
        );
        if (form.getStatus() == PaymentStatus.COMPLETED) {
            invoice.registerCompletedPayment(form.getAmount());
        }
        return payments.save(payment);
    }

    /**
     * Updates an existing payment while preserving invoice balance correctness.
     *
     * <p>If the previous payment state affected the invoice balance, that effect is reversed before the new
     * form values are applied. This prevents duplicated paid amounts when a completed payment is edited.
     *
     * @param id   payment identifier
     * @param form validated payment form
     * @return the updated payment
     */
    @Transactional
    public Payment update(Long id, PaymentForm form) {
        Long companyId = companyContext.currentCompanyId();
        Payment payment = getCurrentCompanyPayment(id);
        Invoice oldInvoice = payment.getInvoice();
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            oldInvoice.removeCompletedPayment(payment.getAmount());
        }

        Invoice invoice = invoices.findByCompanyAccountIdAndIdAndActiveTrue(companyId, form.getInvoiceId())
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada"));
        payment.updateFrom(invoice, form.getAmount(), form.getPaymentDate(), form.getMethod().trim(), form.getStatus());
        if (form.getStatus() == PaymentStatus.COMPLETED) {
            invoice.registerCompletedPayment(form.getAmount());
        }
        return payment;
    }

    /**
     * Marks a pending payment as completed and applies it to the invoice paid amount.
     *
     * @param id payment identifier
     * @return the completed payment
     * @throws IllegalStateException when the payment is not pending or would overpay the invoice
     */
    @Transactional
    public Payment confirm(Long id) {
        Payment payment = getCurrentCompanyPayment(id);
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return payment;
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Solo se pueden confirmar pagos pendientes");
        }
        Invoice invoice = payment.getInvoice();
        if (payment.getAmount().compareTo(invoice.getOutstandingAmount()) > 0) {
            throw new IllegalStateException("El importe no puede superar el pendiente de la factura");
        }
        invoice.registerCompletedPayment(payment.getAmount());
        payment.markCompleted();
        return payment;
    }

    /**
     * Soft-archives a payment and reverses its invoice balance effect when needed.
     *
     * @param id payment identifier
     */
    @Transactional
    public void archive(Long id) {
        Payment payment = getCurrentCompanyPayment(id);
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            payment.getInvoice().removeCompletedPayment(payment.getAmount());
        }
        payment.setActive(false);
    }
}
