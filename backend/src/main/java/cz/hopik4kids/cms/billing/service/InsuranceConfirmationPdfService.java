package cz.hopik4kids.cms.billing.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import cz.hopik4kids.cms.billing.domain.Invoice;
import cz.hopik4kids.cms.billing.domain.SupplierSettings;
import cz.hopik4kids.cms.kernel.pdf.PdfFonts;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.registrations.domain.Child;
import cz.hopik4kids.cms.registrations.domain.Registration;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Renders a health-insurance payment confirmation ("Potvrzení o zaplacení" pro pojišťovnu).
 * Many Czech insurers reimburse part of a child's sport-club fee against such a confirmation.
 * Generated only for paid registrations whose parent requested it (prd todo: insurance confirmation).
 */
@Service
public class InsuranceConfirmationPdfService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Color DARK = new Color(30, 41, 59);
    private static final Color MUTED = new Color(120, 130, 145);
    private static final Color LINE = new Color(220, 225, 232);

    private final SupplierSettingsService supplier;
    private final AuditService audit;

    public InsuranceConfirmationPdfService(SupplierSettingsService supplier, AuditService audit) {
        this.supplier = supplier;
        this.audit = audit;
    }

    /**
     * @param inv        the paid invoice (source of VS + amount + payer)
     * @param reg        the registration (child, insurer, program)
     * @param paidAmount the actual amount paid (override or invoiced total)
     */
    public byte[] build(Invoice inv, Registration reg, int paidAmount) {
        SupplierSettings s = supplier.getOrDefault();
        Child child = reg.getChild();
        String programName = reg.getProgram() != null ? reg.getProgram().getName() : "—";
        LocalDate paidOn = inv.getPaidAt() != null
                ? inv.getPaidAt().atZone(ZoneId.systemDefault()).toLocalDate()
                : LocalDate.now();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 48, 48, 48, 48);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont = PdfFonts.bold(16, DARK);
            Font labelFont = PdfFonts.regular(8, MUTED);
            Font normal = PdfFonts.regular(10, DARK);
            Font boldSm = PdfFonts.bold(10, DARK);
            Font small = PdfFonts.regular(8, MUTED);

            // --- Header: title (left) · logo (right) ---
            PdfPTable head = new PdfPTable(new float[]{3f, 1f});
            head.setWidthPercentage(100);
            PdfPCell titleCell = borderless();
            titleCell.addElement(new Paragraph("POTVRZENÍ O ZAPLACENÍ", titleFont));
            Paragraph sub = new Paragraph("pro zdravotní pojišťovnu", labelFont);
            sub.setSpacingBefore(2);
            titleCell.addElement(sub);
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

            // --- Issuer ---
            Paragraph issuerLabel = new Paragraph("VYSTAVIL", labelFont);
            issuerLabel.setSpacingBefore(14);
            doc.add(issuerLabel);
            doc.add(spaced(new Paragraph(nz(s.getName(), "Hopík4Kids"), PdfFonts.bold(12, DARK)), 3, 0));
            if (has(s.getAddress())) {
                doc.add(new Paragraph(s.getAddress(), normal));
            }
            String issuerMeta = joinNonEmpty(" · ",
                    has(s.getIco()) ? "IČO: " + s.getIco() : null,
                    nzOrNull(s.getEmail()),
                    nzOrNull(s.getWeb()));
            if (has(issuerMeta)) {
                doc.add(spaced(new Paragraph(issuerMeta, small), 2, 0));
            }

            // --- Statement ---
            Paragraph statement = new Paragraph(
                    "Potvrzujeme, že za níže uvedené dítě byla uhrazena úhrada za sportovní kroužek / program.",
                    normal);
            statement.setSpacingBefore(18);
            doc.add(statement);

            // --- Details table ---
            PdfPTable t = new PdfPTable(new float[]{1.3f, 3f});
            t.setWidthPercentage(100);
            t.setSpacingBefore(14);
            row(t, "Dítě", nz(child.getFullName(), "—"), labelFont, boldSm);
            if (child.getBirthDate() != null) {
                row(t, "Datum narození", child.getBirthDate().format(DATE), labelFont, normal);
            }
            row(t, "Zdravotní pojišťovna", nz(child.getHealthInsurance(), "—"), labelFont, normal);
            row(t, "Program", programName, labelFont, normal);
            row(t, "Plátce", nz(inv.getPayerName(), "—"), labelFont, normal);
            row(t, "Variabilní symbol", nz(inv.getVariableSymbol(), "—"), labelFont, normal);
            row(t, "Datum úhrady", paidOn.format(DATE), labelFont, normal);
            row(t, "Zaplacená částka", money(paidAmount) + " Kč", labelFont, PdfFonts.bold(12, DARK));
            doc.add(t);

            // --- Signature area ---
            Paragraph place = new Paragraph(
                    "V " + nz(supplierCity(s), "Plzni") + " dne " + LocalDate.now().format(DATE), normal);
            place.setSpacingBefore(36);
            doc.add(place);
            Paragraph sig = new Paragraph(nz(s.getName(), "Hopík4Kids"), small);
            sig.setSpacingBefore(28);
            sig.setAlignment(Element.ALIGN_RIGHT);
            doc.add(sig);

            if (has(s.getFooterText())) {
                Paragraph footer = new Paragraph(s.getFooterText(), small);
                footer.setSpacingBefore(24);
                doc.add(footer);
            }

            doc.close();
            audit.record("insurance-confirmation-pdf", "Registration", reg.getId());
            return out.toByteArray();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "PDF_ERROR", "Nepodařilo se vytvořit potvrzení pro pojišťovnu");
        }
    }

    // --- helpers ---

    private static void row(PdfPTable t, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell l = new PdfPCell(new Paragraph(label, labelFont));
        l.setBorder(0);
        l.setBorderWidthBottom(1);
        l.setBorderColorBottom(LINE);
        l.setPadding(7);
        l.setPaddingLeft(0);
        t.addCell(l);
        PdfPCell v = new PdfPCell(new Paragraph(value, valueFont));
        v.setBorder(0);
        v.setBorderWidthBottom(1);
        v.setBorderColorBottom(LINE);
        v.setPadding(7);
        t.addCell(v);
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

    private static String supplierCity(SupplierSettings s) {
        // Best-effort: last comma-separated part of the address is usually the city.
        if (!has(s.getAddress())) {
            return null;
        }
        String[] parts = s.getAddress().split(",");
        return parts.length > 0 ? parts[parts.length - 1].trim() : null;
    }

    private static String money(int n) {
        String str = String.valueOf(Math.abs(n));
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = str.length() - 1; i >= 0; i--) {
            sb.append(str.charAt(i));
            if (++count % 3 == 0 && i > 0) sb.append('\u00A0');
        }
        return (n < 0 ? "-" : "") + sb.reverse();
    }

    private static boolean has(String v) {
        return v != null && !v.isBlank();
    }

    private static String nz(String v, String fallback) {
        return has(v) ? v : fallback;
    }

    private static String nzOrNull(String v) {
        return has(v) ? v : null;
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
