package com.lsototalbouw.payment;

import com.lsototalbouw.common.enums.PaymentStatus;
import java.math.BigDecimal;
import java.util.List;

public record PaymentSummary(
        int totalPayments,
        int pendingPayments,
        BigDecimal completedAmount,
        BigDecimal pendingAmount
) {

    public static PaymentSummary from(List<Payment> payments) {
        BigDecimal completedAmount = BigDecimal.ZERO;
        BigDecimal pendingAmount = BigDecimal.ZERO;
        int pendingPayments = 0;

        for (Payment payment : payments) {
            if (payment.getStatus() == PaymentStatus.COMPLETED) {
                completedAmount = completedAmount.add(payment.getAmount());
            } else if (payment.getStatus() == PaymentStatus.PENDING) {
                pendingAmount = pendingAmount.add(payment.getAmount());
                pendingPayments++;
            }
        }

        return new PaymentSummary(payments.size(), pendingPayments, completedAmount, pendingAmount);
    }
}
