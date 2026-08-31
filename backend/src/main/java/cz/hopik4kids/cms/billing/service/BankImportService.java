package cz.hopik4kids.cms.billing.service;

import cz.hopik4kids.cms.billing.domain.BankTransaction;
import cz.hopik4kids.cms.billing.domain.BankTransactionStatus;
import cz.hopik4kids.cms.billing.domain.Invoice;
import cz.hopik4kids.cms.billing.domain.InvoiceStatus;
import cz.hopik4kids.cms.billing.repository.BankTransactionRepository;
import cz.hopik4kids.cms.billing.repository.InvoiceRepository;
import cz.hopik4kids.cms.billing.web.dto.BankMatchDto;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Bank statement import + invoice matching (prd todo #5). Parses a Raiffeisenbank CSV export,
 * proposes matches by variable symbol + amount, and (on confirm) marks invoices paid. Idempotent
 * via the bank's transaction id — re-uploading the same statement never double-pays.
 */
@Service
public class BankImportService {

    private static final DateTimeFormatter CZ_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final InvoiceRepository invoices;
    private final BankTransactionRepository transactions;
    private final InvoiceService invoiceService;
    private final AuditService audit;

    public BankImportService(InvoiceRepository invoices, BankTransactionRepository transactions,
                             InvoiceService invoiceService, AuditService audit) {
        this.invoices = invoices;
        this.transactions = transactions;
        this.invoiceService = invoiceService;
        this.audit = audit;
    }

    /** Parsed row from the statement (before persistence). */
    private record Row(String txId, LocalDate date, BigDecimal amount, String vs,
                       String counterparty, String message) {}

    /** Preview: parse the CSV and propose a match for each incoming payment. No writes. */
    @Transactional(readOnly = true)
    public List<BankMatchDto> preview(MultipartFile file) {
        List<Row> rows = parse(file);
        List<BankMatchDto> out = new ArrayList<>();
        for (Row r : rows) {
            // Skip outgoing payments (negative) — we only match incoming.
            if (r.amount().signum() <= 0) {
                out.add(dto(r, "OUTGOING", null));
                continue;
            }
            // Already imported?
            if (transactions.findByTxId(r.txId()).isPresent()) {
                out.add(dto(r, "ALREADY", null));
                continue;
            }
            Invoice inv = r.vs() == null || r.vs().isBlank() ? null
                    : invoices.findByVariableSymbol(r.vs()).orElse(null);
            if (inv == null) {
                out.add(dto(r, "NONE", null));
            } else if (BigDecimal.valueOf(inv.getTotalAmount()).compareTo(r.amount()) == 0) {
                out.add(dto(r, "EXACT", inv));
            } else {
                out.add(dto(r, "PARTIAL", inv));
            }
        }
        return out;
    }

    /**
     * Confirm the given transactions: persist them and (for matched ones) mark the invoice paid.
     * Idempotent — transactions already imported are skipped. Returns how many invoices were paid.
     */
    @Transactional
    public ConfirmResult confirm(MultipartFile file, List<String> txIdsToApply) {
        List<Row> rows = parse(file);
        int paid = 0;
        int imported = 0;
        for (Row r : rows) {
            if (r.amount().signum() <= 0 || transactions.findByTxId(r.txId()).isPresent()) {
                continue;
            }
            boolean apply = txIdsToApply != null && txIdsToApply.contains(r.txId());
            Invoice inv = r.vs() == null ? null : invoices.findByVariableSymbol(r.vs()).orElse(null);

            BankTransaction bt = new BankTransaction();
            bt.setTxId(r.txId());
            bt.setTxDate(r.date());
            bt.setAmount(r.amount());
            bt.setVariableSymbol(r.vs());
            bt.setCounterparty(r.counterparty());
            bt.setMessage(r.message());

            if (apply && inv != null) {
                if (inv.getStatus() != InvoiceStatus.PAID) {
                    invoiceService.markPaid(inv.getId());
                    paid++;
                }
                bt.setMatchedInvoiceId(inv.getId());
                bt.setStatus(BankTransactionStatus.MATCHED);
            } else {
                bt.setStatus(BankTransactionStatus.IGNORED);
            }
            transactions.save(bt);
            imported++;
        }
        audit.record("bank-import", "BankTransaction", null, "imported=" + imported + " paid=" + paid);
        return new ConfirmResult(imported, paid);
    }

    public record ConfirmResult(int imported, int paid) {}

    // --- parsing ---

    private List<Row> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("EMPTY_FILE", "Soubor je prázdný");
        }
        List<Row> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = br.readLine();
            if (header == null) {
                throw ApiException.badRequest("EMPTY_FILE", "Výpis je prázdný");
            }
            char delim = header.contains(";") ? ';' : ',';
            Cols cols = mapColumns(splitCsv(header, delim));
            if (cols.vs < 0 || cols.amount < 0 || cols.txId < 0) {
                throw ApiException.badRequest("BAD_FORMAT",
                        "Neznámý formát výpisu (chybí VS, částka nebo Id transakce)");
            }
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] c = splitCsv(line, delim);
                String txId = at(c, cols.txId);
                if (txId == null || txId.isBlank()) {
                    continue;
                }
                rows.add(new Row(
                        txId,
                        parseDate(at(c, cols.date)),
                        parseAmount(at(c, cols.amount)),
                        digitsOrNull(at(c, cols.vs)),
                        at(c, cols.counterparty),
                        at(c, cols.message)));
            }
        } catch (IOException e) {
            throw ApiException.badRequest("READ_ERROR", "Soubor se nepodařilo přečíst");
        }
        return rows;
    }

    private record Cols(int date, int amount, int vs, int txId, int counterparty, int message) {}

    /** Map Raiffeisen (and similar) header names to column indexes. */
    private static Cols mapColumns(String[] header) {
        int date = -1, amount = -1, vs = -1, txId = -1, counterparty = -1, message = -1;
        for (int i = 0; i < header.length; i++) {
            String h = header[i].trim().toLowerCase();
            if (date < 0 && h.startsWith("datum provedení")) date = i;
            else if (amount < 0 && h.contains("zaúčtovaná částka")) amount = i;
            else if (h.equals("vs")) vs = i;
            else if (txId < 0 && h.contains("id transakce")) txId = i;
            else if (counterparty < 0 && h.contains("název protiúčtu")) counterparty = i;
            else if (message < 0 && (h.equals("zpráva") || h.equals("poznámka"))) message = i;
        }
        return new Cols(date, amount, vs, txId, counterparty, message);
    }

    /** Split a CSV line honoring double-quoted fields. */
    private static String[] splitCsv(String line, char delim) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == delim && !inQuotes) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    private static String at(String[] arr, int idx) {
        if (idx < 0 || idx >= arr.length) {
            return null;
        }
        String v = arr[idx].trim();
        return v.isEmpty() ? null : v;
    }

    private static LocalDate parseDate(String s) {
        if (s == null) {
            return null;
        }
        // Statement may include a time part ("13.07.2026 13:48"); keep the date only.
        String datePart = s.split(" ")[0].trim();
        try {
            return LocalDate.parse(datePart, CZ_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    private static BigDecimal parseAmount(String s) {
        if (s == null) {
            return BigDecimal.ZERO;
        }
        // Normalize: remove spaces, unify decimal separator.
        String n = s.replace(" ", "").replace("\u00a0", "");
        if (n.contains(",") && n.contains(".")) {
            n = n.replace(".", "").replace(",", "."); // 1.234,56 -> 1234.56
        } else {
            n = n.replace(",", ".");
        }
        try {
            return new BigDecimal(n);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static String digitsOrNull(String s) {
        if (s == null) {
            return null;
        }
        String digits = s.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? null : digits;
    }

    private BankMatchDto dto(Row r, String status, Invoice inv) {
        return new BankMatchDto(
                r.txId(), r.date(), r.amount(), r.vs(), r.counterparty(), r.message(),
                status,
                inv == null ? null : inv.getId(),
                inv == null ? null : inv.getInvoiceNumber(),
                inv == null ? null : inv.getTotalAmount(),
                inv != null && inv.getStatus() == InvoiceStatus.PAID);
    }
}
