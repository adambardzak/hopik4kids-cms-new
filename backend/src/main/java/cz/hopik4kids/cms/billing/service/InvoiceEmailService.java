package cz.hopik4kids.cms.billing.service;

import cz.hopik4kids.cms.billing.web.dto.InvoiceDto;
import cz.hopik4kids.cms.kernel.email.EmailService;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.registrations.domain.PaymentStatus;
import cz.hopik4kids.cms.registrations.repository.RegistrationRepository;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
import org.springframework.stereotype.Service;

/** Sends an invoice PDF to the payer by email (prd §6A.5). */
@Service
public class InvoiceEmailService {

    private final InvoiceService invoices;
    private final InvoicePdfService pdf;
    private final SupplierSettingsService supplier;
    private final EmailService email;
    private final RegistrationRepository registrations;
    private final AuditService audit;

    public InvoiceEmailService(InvoiceService invoices,
                               InvoicePdfService pdf,
                               SupplierSettingsService supplier,
                               EmailService email,
                               RegistrationRepository registrations,
                               AuditService audit) {
        this.invoices = invoices;
        this.pdf = pdf;
        this.supplier = supplier;
        this.email = email;
        this.registrations = registrations;
        this.audit = audit;
    }

    public void send(String invoiceId) {
        sendInternal(invoiceId, false);
    }

    /**
     * Welcome + invoice email sent automatically right after a parent registers (auto-invoicing).
     * Thank-you copy with the invoice attached. Best-effort caller should catch failures.
     */
    public void sendWelcome(String invoiceId, String childName, cz.hopik4kids.cms.core.domain.Program program) {
        sendInternal(invoiceId, true, childName, program);
    }

    private void sendInternal(String invoiceId, boolean welcome) {
        sendInternal(invoiceId, welcome, null, null);
    }

    private static final java.time.format.DateTimeFormatter CZ_DATE =
            java.time.format.DateTimeFormatter.ofPattern("d. M. yyyy");

    private static String czDate(java.time.LocalDate d) {
        return d == null ? "—" : d.format(CZ_DATE);
    }

    /** First lesson date: club/school validFrom, camp startDate. */
    private static String startOf(cz.hopik4kids.cms.core.domain.Program p) {
        if (p == null) return "—";
        return czDate(p.getValidFrom() != null ? p.getValidFrom() : p.getStartDate());
    }

    private static String timeFrom(cz.hopik4kids.cms.core.domain.Program p) {
        return p == null || p.getTime() == null || p.getTime().isBlank() ? "—" : p.getTime();
    }

    private static String timeTo(cz.hopik4kids.cms.core.domain.Program p) {
        if (p == null || p.getTime() == null || p.getDurationMin() == null || p.getDurationMin() <= 0) {
            return "—";
        }
        try {
            return java.time.LocalTime.parse(p.getTime()).plusMinutes(p.getDurationMin()).toString();
        } catch (Exception e) {
            return "—";
        }
    }

    private void sendInternal(String invoiceId, boolean welcome, String childName,
                              cz.hopik4kids.cms.core.domain.Program program) {
        String programName = program != null ? program.getName() : null;
        InvoiceDto inv = invoices.get(invoiceId);
        if (inv.payerEmail() == null || inv.payerEmail().isBlank()) {
            throw ApiException.badRequest("NO_PAYER_EMAIL", "Faktura nemá e-mail plátce");
        }

        byte[] bytes = pdf.build(invoiceId);
        String supplierName = supplier.getOrDefault().getName();
        String sender = supplierName == null ? "Hopík4Kids" : supplierName;

        String body;
        String subject;
        if (welcome) {
            String child = childName != null ? childName : "";
            subject = "Potvrzení přihlášky a podklady k platbě – " + child + " | Hopík4Kids";
            boolean kindergarten = program != null
                    && program.getType() == cz.hopik4kids.cms.core.domain.ProgramType.SCHOOL;
            String kindergartenInfo = kindergarten ? """
                    Organizace v MŠ: Děti si vyzvedáváme přímo ve třídě u paní učitelek a po skončení lekce je v pořádku a včas vracíme zpět paním učitelkám nebo do předem domluvené třídy, kde si je můžete vyzvednout.

                    """ : "";
            body = """
                    Dobrý den, děkujeme Vám za přihlášení %1$s do programu %2$s.

                    Vaši registraci jsme úspěšně přijali.

                    Shrnutí registrace a kroužku:

                    Jméno a příjmení dítěte: %1$s

                    Začátek kroužku: %3$s

                    Čas konání: %4$s – %5$s

                    %6$sPodklady k platbě: V příloze najdete fakturu č. %7$s na částku %8$d Kč se splatností %9$s.

                    Částka k úhradě: %8$d Kč

                    Variabilní symbol: %10$s

                    Splatnost: %9$s

                    Zaplatit můžete bankovním převodem nebo jednoduše naskenováním QR platby přímo z faktury v příloze.

                    Co dětem připravit s sebou:

                    - Sportovní obuv (se světlou / nebarvící podrážkou do tělocvičny)
                    - Pohodlné sportovní oblečení (tričko, kraťasy nebo tepláky), ve kterém se dětem bude dobře hýbat
                    - Láhev s pitím (podepsanou, aby si ji děti nepopletly)

                    Doklad o zaplacení a příspěvek od pojišťovny:

                    Po připsání platby na náš účet Vám automaticky zašleme e-mailem doklad o zaplacení (potvrzení o úhradě). Ten obsahuje všechny potřebné náležitosti pro zdravotní pojišťovny, takže jej můžete ihned využít k žádosti o příspěvek na sportovní aktivitu Vašich dětí. Pokud byste potřebovali s potvrzením cokoliv upravit nebo doplnit, stačí se nám ozvat a rádi Vám pomůžeme!

                    Sledujte, jak nám to sportuje: Fotky a ukázky z našich lekcí sdílíme na náš Instagram Hopík4Kids, tak nás nezapomeňte sledovat!

                    Moc se těšíme, až to společně rozskáčeme a čeká nás skvělé společné sportování plné pohybové zábavy!

                    Pokud budete mít jakýkoliv dotaz, neváhejte nás kontaktovat.

                    Bc. Matěj Fikrle & Bc. Petr Jílek

                    Hopík4Kids s.r.o.

                    +420 730 634 153
                    """.formatted(
                    child,
                    programName != null ? programName : "Hopík4Kids",
                    startOf(program),
                    timeFrom(program),
                    timeTo(program),
                    kindergartenInfo,
                    inv.invoiceNumber(),
                    inv.totalAmount(),
                    czDate(inv.dueDate()),
                    inv.variableSymbol());
        } else {
            subject = "Faktura č. " + inv.invoiceNumber() + " — " + sender;
            body = """
                    Dobrý den,

                    v příloze zasíláme fakturu č. %s na částku %d Kč se splatností %s.
                    Fakturu můžete zaplatit převodem (variabilní symbol %s) nebo naskenováním
                    QR platby přímo z faktury.

                    Děkujeme,
                    %s
                    """.formatted(
                    inv.invoiceNumber(),
                    inv.totalAmount(),
                    inv.dueDate(),
                    inv.variableSymbol(),
                    sender);
        }

        boolean ok = email.sendWithAttachment(
                inv.payerEmail(),
                subject,
                body,
                "faktura-" + inv.invoiceNumber() + ".pdf",
                bytes,
                "application/pdf");

        if (!ok) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "EMAIL_FAILED", "Fakturu se nepodařilo odeslat (zkontrolujte nastavení e-mailu)");
        }
        audit.record("invoice-email", "Invoice", invoiceId, "{\"to\":\"" + inv.payerEmail() + "\"}");

        // Reflect that the invoice was sent on the registration's payment status (unless already paid).
        registrations.findById(inv.registrationId()).ifPresent(reg -> {
            if (reg.getPaymentStatus() == PaymentStatus.UNPAID) {
                reg.setPaymentStatus(PaymentStatus.INVOICE_SENT);
                registrations.save(reg);
            }
        });
    }
}
