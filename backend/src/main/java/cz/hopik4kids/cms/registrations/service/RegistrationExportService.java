package cz.hopik4kids.cms.registrations.service;

import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.registrations.web.dto.AdminRegistrationDto;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Registration export (prd §5.6, §6.3) - full fields for invoicing / attendance.
 * CSV includes a UTF-8 BOM so Excel opens Czech characters correctly.
 */
@Service
public class RegistrationExportService {

    private static final String[] HEADERS = {
            "ID", "Program", "Typ", "Dítě", "Datum narození", "Rodné číslo", "Adresa dítěte",
            "Pojišťovna", "Třída", "Rodič", "Telefon", "E-mail", "Druhý rodič", "Telefon 2",
            "Dres", "Velikost", "Přezdívka", "Alergie", "Poznámka", "Souhlas OÚ", "Souhlas média",
            "Stav platby", "Cena", "Stav", "Zdroj", "Vytvořeno"
    };

    private final AdminRegistrationService registrations;

    public RegistrationExportService(AdminRegistrationService registrations) {
        this.registrations = registrations;
    }

    public byte[] exportCsv(String programId, String paymentStatus) {
        List<AdminRegistrationDto> rows = registrations.list(programId, paymentStatus);
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append(String.join(";", HEADERS)).append('\n');
        for (AdminRegistrationDto r : rows) {
            String[] cells = cells(r);
            for (int i = 0; i < cells.length; i++) {
                if (i > 0) {
                    sb.append(';');
                }
                sb.append(csvEscape(cells[i]));
            }
            sb.append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportXlsx(String programId, String paymentStatus) {
        List<AdminRegistrationDto> rows = registrations.list(programId, paymentStatus);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Registrace");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }
            int rowIdx = 1;
            for (AdminRegistrationDto r : rows) {
                Row row = sheet.createRow(rowIdx++);
                String[] cells = cells(r);
                for (int i = 0; i < cells.length; i++) {
                    row.createCell(i).setCellValue(cells[i] == null ? "" : cells[i]);
                }
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "EXPORT_ERROR", "Export selhal");
        }
    }

    private static String[] cells(AdminRegistrationDto r) {
        return new String[]{
                r.id(), r.programName(), r.programType(), r.childName(),
                str(r.birthDate()), r.personalId(), r.childAddress(), r.healthInsurance(), r.className(),
                r.parentName(), r.parentPhone(), r.parentEmail(), r.secondParentName(), r.secondParentPhone(),
                r.wantsShirt() ? "ano" : "ne", r.shirtSize(), r.nickName(), r.allergies(), r.note(),
                r.consentPersonalData() ? "ano" : "ne", r.consentMedia() ? "ano" : "ne",
                r.paymentStatus(), String.valueOf(r.priceSnapshot()), r.status(), r.source(),
                str(r.createdAt())
        };
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private static String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
