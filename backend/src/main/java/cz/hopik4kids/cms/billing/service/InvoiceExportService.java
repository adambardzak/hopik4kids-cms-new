package cz.hopik4kids.cms.billing.service;

import cz.hopik4kids.cms.billing.web.dto.InvoiceDto;
import cz.hopik4kids.cms.kernel.web.ApiException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * Invoice export (prd todo #2). Columns: number, payer, total, program vs. shirt split,
 * issue/due dates, status. CSV includes a UTF-8 BOM so Excel opens Czech characters correctly.
 */
@Service
public class InvoiceExportService {

    private static final String[] HEADERS = {
            "Číslo faktury", "Odběratel", "Částka celkem", "Kroužek", "Dres",
            "Datum vystavení", "Datum splatnosti", "Variabilní symbol", "Stav"
    };

    private static final java.util.Map<String, String> STATUS_CS = java.util.Map.of(
            "paid", "Zaplaceno", "unpaid", "Nezaplaceno", "cancelled", "Storno");

    private final InvoiceService invoices;

    public InvoiceExportService(InvoiceService invoices) {
        this.invoices = invoices;
    }

    public byte[] exportCsv(LocalDate from, LocalDate to, String status, String type) {
        List<InvoiceDto> rows = invoices.list(from, to, status, type);
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append(String.join(";", HEADERS)).append('\n');
        for (InvoiceDto r : rows) {
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

    public byte[] exportXlsx(LocalDate from, LocalDate to, String status, String type) {
        List<InvoiceDto> rows = invoices.list(from, to, status, type);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Faktury");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }
            int rowIdx = 1;
            for (InvoiceDto r : rows) {
                Row row = sheet.createRow(rowIdx++);
                String[] cells = cells(r);
                for (int i = 0; i < cells.length; i++) {
                    row.createCell(i).setCellValue(cells[i] == null ? "" : cells[i]);
                }
            }
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "EXPORT_ERROR", "Export selhal");
        }
    }

    private static String[] cells(InvoiceDto r) {
        return new String[]{
                r.invoiceNumber(),
                r.payerName(),
                String.valueOf(r.totalAmount()),
                String.valueOf(r.programAmount()),
                String.valueOf(r.shirtAmount()),
                str(r.issueDate()),
                str(r.dueDate()),
                r.variableSymbol(),
                STATUS_CS.getOrDefault(r.status(), r.status())
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
