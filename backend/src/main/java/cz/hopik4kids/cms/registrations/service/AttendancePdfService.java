package cz.hopik4kids.cms.registrations.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import cz.hopik4kids.cms.core.domain.Program;
import cz.hopik4kids.cms.core.repository.ProgramRepository;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.registrations.web.dto.AdminRegistrationDto;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Attendance / contact list PDF per program (prd §6A.3): children + contacts + allergies +
 * consents, ready to print and take to a lesson/camp. One click instead of assembling from emails.
 */
@Service
public class AttendancePdfService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d.M.yyyy");

    private final ProgramRepository programs;
    private final AdminRegistrationService registrations;
    private final AuditService audit;

    public AttendancePdfService(ProgramRepository programs,
                                AdminRegistrationService registrations,
                                AuditService audit) {
        this.programs = programs;
        this.registrations = registrations;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public byte[] build(String programId) {
        // Trainers may only print their assigned programs (prd §7.5).
        if (!cz.hopik4kids.cms.kernel.web.SecurityUtils.isPrivileged()) {
            String uid = cz.hopik4kids.cms.kernel.web.SecurityUtils.currentUserId();
            if (uid == null || !programs.isTrainerAssigned(programId, uid)) {
                throw ApiException.forbidden("NOT_ASSIGNED", "Nemáš přístup k tomuto programu");
            }
        }
        Program program = programs.findByIdWithLocation(programId)
                .orElseThrow(() -> ApiException.notFound("Program nenalezen"));
        List<AdminRegistrationDto> rows = registrations.list(programId, null, null).stream()
                .filter(r -> !"cancelled".equals(r.status()))
                .toList();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);

            doc.add(new Paragraph(program.getName(), titleFont));
            StringBuilder meta = new StringBuilder();
            if (program.getLocation() != null) {
                meta.append(program.getLocation().getName());
                if (program.getLocation().getAddress() != null) {
                    meta.append(", ").append(program.getLocation().getAddress());
                }
            }
            meta.append(meta.length() > 0 ? "  ·  " : "")
                    .append("Počet dětí: ").append(rows.size());
            if (program.getCapacity() != null) {
                meta.append(" / ").append(program.getCapacity());
            }
            meta.append("  ·  Vytištěno: ").append(LocalDate.now().format(DATE));
            doc.add(new Paragraph(meta.toString(), metaFont));
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{3f, 2.2f, 3.5f, 3f, 1.4f, 1.6f, 2.5f});
            table.setWidthPercentage(100);

            String[] headers = {"Dítě", "Narození", "Rodič + telefon", "Alergie / zdrav.",
                    "Dres", "Souhlasy", "Docházka"};
            Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
                cell.setBackgroundColor(new Color(37, 99, 235));
                cell.setPadding(5);
                table.addCell(cell);
            }

            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            boolean alt = false;
            for (AdminRegistrationDto r : rows) {
                Color bg = alt ? new Color(245, 247, 250) : Color.WHITE;
                alt = !alt;

                addCell(table, r.childName(), cellFont, bg);
                addCell(table, r.birthDate() == null ? "" : r.birthDate().format(DATE), cellFont, bg);
                addCell(table, r.parentName() + "\n" + nz(r.parentPhone()), cellFont, bg);
                addCell(table, nz(r.allergies()), cellFont, bg);
                addCell(table, r.wantsShirt() ? nz(r.shirtSize(), "ano") : "—", cellFont, bg);
                String consents = (r.consentPersonalData() ? "OÚ ✓" : "OÚ ✗")
                        + "  " + (r.consentMedia() ? "foto ✓" : "foto ✗");
                addCell(table, consents, cellFont, bg);
                addCell(table, "", cellFont, bg); // empty column to tick attendance by hand
            }

            doc.add(table);
            doc.close();

            audit.record("attendance-pdf", "Program", programId);
            return out.toByteArray();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "PDF_ERROR", "Nepodařilo se vytvořit PDF");
        }
    }

    private void addCell(PdfPTable table, String text, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(4);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String nz(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }
}
