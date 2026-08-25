package cz.hopik4kids.cms.billing.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import cz.hopik4kids.cms.billing.domain.Invoice;
import cz.hopik4kids.cms.billing.domain.SupplierSettings;
import cz.hopik4kids.cms.kernel.pdf.PdfFonts;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Renders an invoice as a PDF with the SPAYD QR payment (prd §6A.5). Styled like a tax document. */
@Service
public class InvoicePdfService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Color DARK = new Color(30, 41, 59);
    private static final Color MUTED = new Color(120, 130, 145);
    private static final Color LINE = new Color(220, 225, 232);
    private static final Color STRIP = new Color(244, 246, 249);

    private final InvoiceService invoices;
    private final SupplierSettingsService supplier;
    private final AuditService audit;

    public InvoicePdfService(InvoiceService invoices,
                             SupplierSettingsService supplier,
                             AuditService audit) {
        this.invoices = invoices;
        this.supplier = supplier;
        this.audit = audit;
    }

    @Transactional
    public byte[] build(String invoiceId) {
        Invoice inv = invoices.find(invoiceId);
        SupplierSettings s = supplier.getOrDefault();
        List<InvoiceService.Item> items = invoices.readItems(inv.getItems());

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 42, 42, 42, 42);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont = PdfFonts.bold(14, DARK);
            Font numberFont = PdfFonts.bold(20, DARK);
            Font labelFont = PdfFonts.regular(8, MUTED);
            Font normal = PdfFonts.regular(9, DARK);
            Font small = PdfFonts.regular(8, MUTED);
            Font boldSm = PdfFonts.bold(9, DARK);

            // --- Header: title + number (left) · logo (right) ---
            PdfPTable head = new PdfPTable(new float[]{3f, 1f});
            head.setWidthPercentage(100);

            PdfPCell titleCell = borderless();
            titleCell.addElement(new Paragraph("FAKTURA — DAŇOVÝ DOKLAD", titleFont));
            Paragraph num = new Paragraph(inv.getInvoiceNumber(), numberFont);
            num.setSpacingBefore(2);
            titleCell.addElement(num);
            head.addCell(titleCell);

            PdfPCell logoCell = borderless();
            logoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            try {
                byte[] logoBytes = new ClassPathResource("logo.png").getInputStream().readAllBytes();
                Image logo = Image.getInstance(logoBytes);
                logo.scaleToFit(56, 56);
                logo.setAlignment(Element.ALIGN_RIGHT);
                logoCell.addElement(logo);
            } catch (Exception ignored) {
                // logo optional
            }
            head.addCell(logoCell);
            doc.add(head);

            doc.add(hr());

            // --- Supplier + payer ---
            PdfPTable parties = new PdfPTable(2);
            parties.setWidthPercentage(100);
            parties.setSpacingBefore(10);

            PdfPCell sup = borderless();
            sup.addElement(new Paragraph("DODAVATEL", labelFont));
            sup.addElement(spaced(new Paragraph(nz(s.getName()), PdfFonts.bold(11, DARK)), 3, 0));
            if (has(s.getAddress())) sup.addElement(new Paragraph(s.getAddress(), normal));
            String supMeta = joinNonEmpty(" · ",
                    has(s.getIco()) ? "IČO: " + s.getIco() : null,
                    (has(s.getDic()) ? "DIČ: " + s.getDic() : "Neplátce DPH"));
            sup.addElement(spaced(new Paragraph(supMeta, small), 2, 0));
            String contact = joinNonEmpty(" · ", nzOrNull(s.getWeb()), nzOrNull(s.getEmail()));
            if (has(contact)) sup.addElement(new Paragraph(contact, small));
            parties.addCell(sup);

            PdfPCell pay = borderless();
            pay.addElement(new Paragraph("ODBĚRATEL", labelFont));
            pay.addElement(spaced(new Paragraph(nz(inv.getPayerName()), PdfFonts.bold(11, DARK)), 3, 0));
            if (has(inv.getPayerAddress())) pay.addElement(new Paragraph(inv.getPayerAddress(), normal));
            if (has(inv.getPayerEmail())) pay.addElement(spaced(new Paragraph(inv.getPayerEmail(), small), 2, 0));
            parties.addCell(pay);
            doc.add(parties);

            // --- Meta strip: dates, payment, VS ---
            PdfPTable meta = new PdfPTable(4);
            meta.setWidthPercentage(100);
            meta.setSpacingBefore(14);
            meta.addCell(metaCell("Datum vystavení", inv.getIssueDate().format(DATE), labelFont, boldSm));
            meta.addCell(metaCell("Datum splatnosti", inv.getDueDate().format(DATE), labelFont, boldSm));
            meta.addCell(metaCell("Forma úhrady", "Převodem", labelFont, boldSm));
            meta.addCell(metaCell("Variabilní symbol", inv.getVariableSymbol(), labelFont, boldSm));
            doc.add(meta);
            if (has(s.getAccountNumber())) {
                PdfPTable acc = new PdfPTable(1);
                acc.setWidthPercentage(100);
                acc.addCell(metaCell("Číslo bankovního účtu", s.getAccountNumber(), labelFont, boldSm));
                doc.add(acc);
            }

            // --- Items ---
            PdfPTable table = new PdfPTable(new float[]{5f, 1.2f, 2f, 2f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(16);
            table.addCell(headCell("Položka", Element.ALIGN_LEFT));
            table.addCell(headCell("Množství", Element.ALIGN_RIGHT));
            table.addCell(headCell("Cena za MJ", Element.ALIGN_RIGHT));
            table.addCell(headCell("Celkem [CZK]", Element.ALIGN_RIGHT));
            for (InvoiceService.Item it : items) {
                table.addCell(bodyCell(it.label(), boldSm, Element.ALIGN_LEFT));
                table.addCell(bodyCell(fmt(it.qty()) + ",00", normal, Element.ALIGN_RIGHT));
                table.addCell(bodyCell(money(it.unitPrice()), normal, Element.ALIGN_RIGHT));
                table.addCell(bodyCell(money(it.qty() * it.unitPrice()), normal, Element.ALIGN_RIGHT));
            }
            doc.add(table);

            // --- Total (right aligned) ---
            PdfPTable total = new PdfPTable(new float[]{3f, 2f});
            total.setWidthPercentage(100);
            total.setSpacingBefore(4);
            total.addCell(borderless()); // spacer
            PdfPCell totalCell = borderless();
            totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalCell.addElement(rightPara(new Paragraph("Cena v CZK", labelFont)));
            totalCell.addElement(rightPara(new Paragraph(money(inv.getTotalAmount()), PdfFonts.bold(16, DARK))));
            total.addCell(totalCell);
            doc.add(total);

            // --- QR payment ---
            if (has(s.getIban())) {
                String spayd = SpaydQr.spayd(s.getIban(), inv.getTotalAmount(),
                        inv.getVariableSymbol(), "Faktura " + inv.getInvoiceNumber());
                byte[] qr = SpaydQr.qrPng(spayd, 240);
                Image qrImg = Image.getInstance(qr);
                qrImg.scaleAbsolute(110, 110);
                PdfPTable qrTable = new PdfPTable(new float[]{1.2f, 4f});
                qrTable.setWidthPercentage(100);
                qrTable.setSpacingBefore(24);
                PdfPCell qrCell = new PdfPCell(qrImg, false);
                qrCell.setBorder(0);
                qrCell.setPadding(2);
                qrTable.addCell(qrCell);
                PdfPCell qrText = borderless();
                qrText.setVerticalAlignment(Element.ALIGN_BOTTOM);
                qrText.addElement(new Paragraph("QR Platba", boldSm));
                qrText.addElement(new Paragraph("IBAN: " + s.getIban(), small));
                qrText.addElement(new Paragraph("VS: " + inv.getVariableSymbol(), small));
                qrTable.addCell(qrText);
                doc.add(qrTable);
            }

            // --- Footer ---
            if (has(s.getFooterText())) {
                Paragraph footer = new Paragraph(s.getFooterText(), small);
                footer.setSpacingBefore(28);
                doc.add(footer);
            }

            doc.close();
            audit.record("invoice-pdf", "Invoice", invoiceId);
            return out.toByteArray();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "PDF_ERROR", "Nepodařilo se vytvořit fakturu");
        }
    }

    // --- cell helpers ---

    private PdfPCell headCell(String text, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, PdfFonts.regular(8, MUTED)));
        c.setBorder(0);
        c.setBorderWidthBottom(1);
        c.setBorderColorBottom(LINE);
        c.setHorizontalAlignment(align);
        c.setPaddingBottom(6);
        c.setPaddingTop(2);
        return c;
    }

    private PdfPCell bodyCell(String text, Font font, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBorder(0);
        c.setBorderWidthBottom(1);
        c.setBorderColorBottom(LINE);
        c.setHorizontalAlignment(align);
        c.setPadding(7);
        c.setPaddingLeft(align == Element.ALIGN_LEFT ? 0 : 7);
        c.setPaddingRight(align == Element.ALIGN_RIGHT ? 0 : 7);
        return c;
    }

    private PdfPCell metaCell(String label, String value, Font labelFont, Font valueFont) {
        PdfPCell c = new PdfPCell();
        c.setBorder(0);
        c.setBackgroundColor(STRIP);
        c.setPadding(8);
        c.addElement(new Paragraph(label, labelFont));
        c.addElement(spaced(new Paragraph(value, valueFont), 2, 0));
        return c;
    }

    private PdfPCell borderless() {
        PdfPCell c = new PdfPCell();
        c.setBorder(0);
        return c;
    }

    private PdfPTable hr() {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(8);
        PdfPCell c = new PdfPCell();
        c.setBorder(0);
        c.setBorderWidthTop(1);
        c.setBorderColorTop(LINE);
        c.setFixedHeight(1);
        t.addCell(c);
        return t;
    }

    private Paragraph spaced(Paragraph p, float before, float after) {
        p.setSpacingBefore(before);
        p.setSpacingAfter(after);
        return p;
    }

    private Paragraph rightPara(Paragraph p) {
        p.setAlignment(Element.ALIGN_RIGHT);
        return p;
    }

    // --- format helpers ---

    private static String money(int amount) {
        return fmt(amount) + ",00";
    }

    private static String fmt(int n) {
        // Thousands separated by a non-breaking space, Czech style.
        String s = String.valueOf(Math.abs(n));
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            sb.append(s.charAt(i));
            if (++count % 3 == 0 && i > 0) sb.append('\u00A0');
        }
        return (n < 0 ? "-" : "") + sb.reverse();
    }

    private static boolean has(String s) {
        return s != null && !s.isBlank();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String nzOrNull(String s) {
        return has(s) ? s : null;
    }

    private static String joinNonEmpty(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            if (sb.length() > 0) sb.append(sep);
            sb.append(p);
        }
        return sb.toString();
    }
}
