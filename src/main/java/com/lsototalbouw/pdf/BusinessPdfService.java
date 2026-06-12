package com.lsototalbouw.pdf;

import com.lsototalbouw.company.CompanyAccount;
import com.lsototalbouw.invoice.Invoice;
import com.lsototalbouw.invoice.InvoiceLine;
import com.lsototalbouw.quotation.Quotation;
import com.lsototalbouw.quotation.QuotationLine;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BusinessPdfService {

    private static final Color BRAND_DARK = new Color(31, 41, 55);
    private static final Color BRAND_BLUE = new Color(37, 99, 235);
    private static final Color LIGHT_BORDER = new Color(229, 231, 235);
    private static final Color LIGHT_BACKGROUND = new Color(248, 250, 252);

    private final Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, BRAND_DARK);
    private final Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BRAND_DARK);
    private final Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BRAND_DARK);
    private final Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
    private final Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

    public byte[] invoicePdf(Invoice invoice, List<InvoiceLine> lines) {
        return renderDocument("FACTURA", invoice.getInvoiceNumber(), invoice.getCompanyAccount(),
                invoice.getCustomer().getName(), invoice.getIssueDate(), invoice.getDueDate(),
                invoice.getProject() == null ? null : invoice.getProject().getName(), invoice.getAmount(),
                invoice.getPaidAmount(), lines.stream().map(PdfLine::from).toList());
    }

    public byte[] quotationPdf(Quotation quotation, List<QuotationLine> lines) {
        return renderDocument("PRESUPUESTO", quotation.getQuotationNumber(), quotation.getCompanyAccount(),
                quotation.getCustomer().getName(), quotation.getIssueDate(), quotation.getExpiryDate(),
                quotation.getTitle(), quotation.getAmount(), null, lines.stream().map(PdfLine::from).toList());
    }

    public byte[] paymentReminderPdf(Invoice invoice) {
        return paymentReminderPdf(invoice, null);
    }

    public byte[] paymentReminderPdf(Invoice invoice, String customMessage) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 42, 42, 42, 42);
            PdfWriter.getInstance(document, output);
            document.open();

            addHeader(document, "RECLAMACION DE PAGO", invoice.getInvoiceNumber(), invoice.getCompanyAccount());
            addPaymentReminderBody(document, invoice, customMessage);
            addFooter(document);

            document.close();
            return output.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("No se pudo generar el PDF de reclamacion", ex);
        }
    }

    private byte[] renderDocument(String documentType, String number, CompanyAccount company, String customerName,
                                  LocalDate issueDate, LocalDate dueDate, String subject, BigDecimal amount,
                                  BigDecimal paidAmount, List<PdfLine> lines) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 36, 36, 36, 42);
            PdfWriter.getInstance(document, output);
            document.open();

            addHeader(document, documentType, number, company);
            addParties(document, customerName, issueDate, dueDate, subject, company);
            addLines(document, lines, amount);
            addTotals(document, amount, paidAmount);
            addFooter(document);

            document.close();
            return output.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("No se pudo generar el PDF", ex);
        }
    }

    private void addHeader(Document document, String documentType, String number, CompanyAccount company) {
        PdfPTable table = new PdfPTable(new float[]{2.2f, 1f});
        table.setWidthPercentage(100);

        PdfPCell companyCell = borderlessCell();
        Paragraph brand = new Paragraph(company.getName(), titleFont);
        brand.setSpacingAfter(6);
        companyCell.addElement(brand);
        addSmallLine(companyCell, company.getAddress());
        addSmallLine(companyCell, company.getEmail());
        addSmallLine(companyCell, company.getPhone());
        addSmallLine(companyCell, label("BTW", company.getVatNumber()));
        addSmallLine(companyCell, label("KvK", company.getKvkNumber()));
        addSmallLine(companyCell, label("IBAN", company.getIban()));

        PdfPCell documentCell = borderlessCell();
        documentCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph type = new Paragraph(documentType, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BRAND_BLUE));
        type.setAlignment(Element.ALIGN_RIGHT);
        Paragraph numberText = new Paragraph(number, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BRAND_DARK));
        numberText.setAlignment(Element.ALIGN_RIGHT);
        documentCell.addElement(type);
        documentCell.addElement(numberText);

        table.addCell(companyCell);
        table.addCell(documentCell);
        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void addParties(Document document, String customerName, LocalDate issueDate, LocalDate dueDate,
                            String subject, CompanyAccount company) {
        PdfPTable table = new PdfPTable(new float[]{1f, 1f});
        table.setWidthPercentage(100);

        PdfPCell customerCell = panelCell();
        customerCell.addElement(new Paragraph("Cliente", sectionFont));
        customerCell.addElement(new Paragraph(customerName, normalFont));

        PdfPCell metaCell = panelCell();
        metaCell.addElement(new Paragraph("Datos del documento", sectionFont));
        metaCell.addElement(new Paragraph("Emision: " + value(issueDate), normalFont));
        metaCell.addElement(new Paragraph("Vencimiento/validez: " + value(dueDate), normalFont));
        metaCell.addElement(new Paragraph("Plazo de pago: " + company.getPaymentTermsDays() + " d", normalFont));
        if (subject != null && !subject.isBlank()) {
            metaCell.addElement(new Paragraph("Referencia: " + subject, normalFont));
        }

        table.addCell(customerCell);
        table.addCell(metaCell);
        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void addLines(Document document, List<PdfLine> lines, BigDecimal fallbackAmount) {
        Paragraph title = new Paragraph("Detalle", sectionFont);
        title.setSpacingAfter(8);
        document.add(title);

        PdfPTable table = new PdfPTable(new float[]{3.6f, 0.8f, 1f, 0.8f, 1f, 1f});
        table.setWidthPercentage(100);
        addHeaderCell(table, "Descripcion");
        addHeaderCell(table, "Cant.");
        addHeaderCell(table, "Precio");
        addHeaderCell(table, "IVA");
        addHeaderCell(table, "Subtotal");
        addHeaderCell(table, "Total");

        if (lines.isEmpty()) {
            addBodyCell(table, "Importe general del documento");
            addBodyCell(table, "1,00");
            addBodyCell(table, money(fallbackAmount));
            addBodyCell(table, "0 %");
            addBodyCell(table, money(fallbackAmount));
            addBodyCell(table, money(fallbackAmount));
        } else {
            for (PdfLine line : lines) {
                addBodyCell(table, line.description());
                addBodyCell(table, decimal(line.quantity()));
                addBodyCell(table, money(line.unitPrice()));
                addBodyCell(table, decimal(line.vatRate()) + " %");
                addBodyCell(table, money(line.subtotal()));
                addBodyCell(table, money(line.total()));
            }
        }
        document.add(table);
    }

    private void addTotals(Document document, BigDecimal amount, BigDecimal paidAmount) {
        PdfPTable table = new PdfPTable(new float[]{2f, 1f});
        table.setWidthPercentage(45);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setSpacingBefore(14);

        addTotalCell(table, "Total");
        addTotalCell(table, money(amount));
        if (paidAmount != null) {
            addTotalCell(table, "Cobrado");
            addTotalCell(table, money(paidAmount));
            addTotalCell(table, "Pendiente");
            addTotalCell(table, money(amount.subtract(paidAmount)));
        }
        document.add(table);
    }

    private void addPaymentReminderBody(Document document, Invoice invoice, String customMessage) {
        BigDecimal outstanding = invoice.getAmount().subtract(invoice.getPaidAmount());
        long overdueDays = invoice.getDueDate() != null && invoice.getDueDate().isBefore(LocalDate.now())
                ? ChronoUnit.DAYS.between(invoice.getDueDate(), LocalDate.now())
                : 0;

        PdfPTable meta = new PdfPTable(new float[]{1f, 1f});
        meta.setWidthPercentage(100);

        PdfPCell customerCell = panelCell();
        customerCell.addElement(new Paragraph("Cliente", sectionFont));
        customerCell.addElement(new Paragraph(invoice.getCustomer().getName(), normalFont));

        PdfPCell invoiceCell = panelCell();
        invoiceCell.addElement(new Paragraph("Factura pendiente", sectionFont));
        invoiceCell.addElement(new Paragraph("Factura: " + invoice.getInvoiceNumber(), normalFont));
        invoiceCell.addElement(new Paragraph("Emision: " + value(invoice.getIssueDate()), normalFont));
        invoiceCell.addElement(new Paragraph("Vencimiento: " + value(invoice.getDueDate()), normalFont));
        invoiceCell.addElement(new Paragraph("Dias vencida: " + overdueDays, normalFont));

        meta.addCell(customerCell);
        meta.addCell(invoiceCell);
        document.add(meta);
        document.add(Chunk.NEWLINE);

        Paragraph subject = new Paragraph("Asunto: recordatorio de pago pendiente", sectionFont);
        subject.setSpacingAfter(10);
        document.add(subject);

        Paragraph body = new Paragraph();
        body.setFont(normalFont);
        body.setLeading(15);
        body.add("Estimado cliente,\n\n");
        body.add("Segun nuestra administracion, la factura " + invoice.getInvoiceNumber()
                + " sigue pendiente de pago. El importe pendiente actualmente es " + money(outstanding) + ".\n\n");
        body.add("Le rogamos que revise esta factura y realice el pago lo antes posible. ");
        if (invoice.getCompanyAccount().getIban() != null && !invoice.getCompanyAccount().getIban().isBlank()) {
            body.add("Puede realizar el pago en la cuenta IBAN "
                    + invoice.getCompanyAccount().getIban() + ", indicando como referencia la factura "
                    + invoice.getInvoiceNumber() + ".\n\n");
        } else {
            body.add("Indique como referencia la factura " + invoice.getInvoiceNumber() + ".\n\n");
        }
        body.add("Si el pago ya ha sido realizado, puede ignorar este recordatorio o enviarnos el justificante para actualizar la administracion.\n\n");
        if (customMessage != null && !customMessage.isBlank()) {
            body.add("Nota adicional:\n" + customMessage.trim() + "\n\n");
        }
        body.add("Atentamente,\n" + invoice.getCompanyAccount().getName());
        document.add(body);

        addTotals(document, invoice.getAmount(), invoice.getPaidAmount());
    }

    private void addFooter(Document document) {
        Paragraph footer = new Paragraph("Documento generado por LSOTOTALBOUW Business Manager.", smallFont);
        footer.setSpacingBefore(28);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    private PdfPCell borderlessCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(4);
        return cell;
    }

    private PdfPCell panelCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(LIGHT_BORDER);
        cell.setBackgroundColor(LIGHT_BACKGROUND);
        cell.setPadding(12);
        return cell;
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, tableHeaderFont));
        cell.setBackgroundColor(BRAND_DARK);
        cell.setBorderColor(BRAND_DARK);
        cell.setPadding(7);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, normalFont));
        cell.setBorderColor(LIGHT_BORDER);
        cell.setPadding(7);
        table.addCell(cell);
    }

    private void addTotalCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, sectionFont));
        cell.setBorderColor(LIGHT_BORDER);
        cell.setPadding(8);
        table.addCell(cell);
    }

    private void addSmallLine(PdfPCell cell, String value) {
        if (value != null && !value.isBlank()) {
            cell.addElement(new Paragraph(value, smallFont));
        }
    }

    private String value(LocalDate date) {
        return date == null ? "-" : date.toString();
    }

    private String label(String label, String value) {
        return value == null || value.isBlank() ? null : label + ": " + value;
    }

    private String money(BigDecimal value) {
        return "EUR " + decimal(value);
    }

    private String decimal(BigDecimal value) {
        return value == null ? "0,00" : String.format("%.2f", value).replace('.', ',');
    }

    private record PdfLine(String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal vatRate,
                           BigDecimal subtotal, BigDecimal total) {
        static PdfLine from(InvoiceLine line) {
            return new PdfLine(line.getDescription(), line.getQuantity(), line.getUnitPrice(), line.getVatRate(),
                    line.getSubtotal(), line.getTotal());
        }

        static PdfLine from(QuotationLine line) {
            return new PdfLine(line.getDescription(), line.getQuantity(), line.getUnitPrice(), line.getVatRate(),
                    line.getSubtotal(), line.getTotal());
        }
    }
}
