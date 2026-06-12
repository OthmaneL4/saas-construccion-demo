package com.lsototalbouw.common.service;

import java.time.LocalDate;
import java.util.Collection;
import org.springframework.stereotype.Service;

/**
 * Service utility responsible for generating unique sequential business numbers.
 *
 * <p>This generator is typically used for generating standardized document identifiers
 * such as invoices and quotations (e.g., {@code INV-2026-0001}).
 */
@Service
public class BusinessNumberGenerator {

    /**
     * Generates the next sequential business number based on a prefix and existing records.
     *
     * <p>The format of the generated number is {@code PREFIX-YYYY-XXXX}, where:
     * <ul>
     *   <li>{@code PREFIX} is the domain prefix provided (e.g., "INV" for invoices, "OFF" for offers/quotations).</li>
     *   <li>{@code YYYY} is the current calendar year.</li>
     *   <li>{@code XXXX} is a 4-digit zero-padded sequence number (e.g., 0001, 0012).</li>
     * </ul>
     *
     * <p>This method filters existing numbers matching the {@code PREFIX-YYYY-} pattern,
     * extracts their sequence numbers, finds the maximum value, and increments it by 1.
     * If no existing sequence is found, it starts at 1.
     *
     * @param prefix          the prefix designating the document type (e.g., "INV" for invoice, "OFF" for quotation)
     * @param existingNumbers a collection of already existing document numbers under the tenant scope
     * @return the newly generated sequential document number string
     */
    public String nextNumber(String prefix, Collection<String> existingNumbers) {
        int year = LocalDate.now().getYear();
        String numberPrefix = prefix + "-" + year + "-";
        int nextSequence = existingNumbers.stream()
                .filter(number -> number != null && number.startsWith(numberPrefix))
                .map(number -> number.substring(numberPrefix.length()))
                .map(this::parseSequence)
                .max(Integer::compareTo)
                .orElse(0) + 1;
        return numberPrefix + String.format("%04d", nextSequence);
    }

    /**
     * Helper method to parse a string representation of a sequence number to an integer.
     *
     * @param value the string sequence value to parse
     * @return the parsed integer value, or {@code 0} if a {@link NumberFormatException} is encountered
     */
    private int parseSequence(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
