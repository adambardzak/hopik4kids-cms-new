package cz.hopik4kids.cms.kernel.email;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * System email transport (prd §8): invitations, password reset, notifications, invoices.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String appBaseUrl;
    private final EmailLogRepository emailLog;

    public EmailService(JavaMailSender mailSender, EmailLogRepository emailLog,
                        @Value("${app.email.from:no-reply@hopik4kids.cz}") String from,
                        @Value("${app.admin-base-url:http://localhost:3000}") String appBaseUrl) {
        this.mailSender = mailSender;
        this.emailLog = emailLog;
        this.from = from;
        this.appBaseUrl = appBaseUrl;
    }

    public void sendInvitation(String to, String inviterName, String token) {
        String link = appBaseUrl + "/pozvanka?token=" + token;
        String body = """
                Ahoj,

                %s tě zve do administrace Hopík4Kids.
                Účet aktivuješ nastavením hesla zde:

                %s

                Odkaz je platný omezenou dobu.
                """.formatted(inviterName, link);
        send(to, "Pozvánka do Hopík4Kids", body);
    }

    public void sendPasswordReset(String to, String token) {
        String link = appBaseUrl + "/reset-hesla?token=" + token;
        String body = """
                Ahoj,

                požádal(a) jsi o obnovení hesla. Nové heslo si nastavíš zde:

                %s

                Pokud jsi o reset nežádal(a), tento e-mail ignoruj.
                """.formatted(link);
        send(to, "Obnovení hesla — Hopík4Kids", body);
    }

    /** Plain-text email. Returns true on success. */
    public boolean send(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, renderHtml(body));
            addLogo(helper);
            mailSender.send(message);
            record(to, subject, null, null);
            return true;
        } catch (Exception e) {
            log.error("Failed to send email '{}' to {}: {}", subject, to, e.getMessage());
            record(to, subject, e.getMessage(), null);
            return false;
        }
    }

    /** Email with a single binary attachment (e.g. invoice PDF). Returns true on success. */
    public boolean sendWithAttachment(String to, String subject, String body,
                                      String attachmentName, byte[] attachment, String contentType) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, renderHtml(body));
            addLogo(helper);
            helper.addAttachment(attachmentName, new ByteArrayResource(attachment), contentType);
            mailSender.send(message);
            record(to, subject, null, attachmentName);
            return true;
        } catch (Exception e) {
            log.error("Failed to send email with attachment '{}' to {}: {}", subject, to, e.getMessage());
            record(to, subject, e.getMessage(), attachmentName);
            return false;
        }
    }

    /** Persist the attempt; never lets logging break the send flow. */
    private void record(String to, String subject, String error, String attachment) {
        try {
            EmailLog l = new EmailLog();
            l.setRecipient(to == null ? "" : to);
            l.setSubject(subject == null ? "" : (subject.length() > 500 ? subject.substring(0, 500) : subject));
            l.setSuccess(error == null);
            l.setError(error);
            l.setAttachment(attachment);
            emailLog.save(l);
        } catch (Exception e) {
            log.warn("Could not write email log: {}", e.getMessage());
        }
    }

    private static final String NAVY = "#1A2B47";

    private void addLogo(MimeMessageHelper helper) {
        try {
            helper.addInline("logo", new org.springframework.core.io.ClassPathResource("logo.png"), "image/png");
        } catch (Exception ignored) {
            // logo is cosmetic
        }
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String inline(String line) {
        String t = esc(line);
        t = t.replaceAll("(https?://[^\\s<]+)", "<a href=\"$1\" style=\"color:" + NAVY + ";\">$1</a>");
        return t.replaceFirst("^([^:<]{2,45}):( |$)", "<strong>$1:</strong> ");
    }

    private static final String YELLOW = "#F5C518";

    /** Wraps a plain-text body (paragraphs, "- " bullets, "Label: value" lines) in a branded HTML layout. */
    static String renderHtml(String body) {
        StringBuilder c = new StringBuilder();
        StringBuilder rows = new StringBuilder();
        for (String block : body.strip().split("\\n\\s*\\n")) {
            String[] lines = block.strip().split("\\n");
            boolean list = java.util.Arrays.stream(lines).allMatch(l -> l.strip().startsWith("- "));
            boolean fact = lines.length == 1 && lines[0].matches("^[^:]{2,45}: .{1,80}$");
            if (fact) {
                int i = lines[0].indexOf(": ");
                rows.append("<tr><td style=\"padding:8px 12px;color:#6b7280;font-size:13px;border-bottom:1px solid #e3e8f0;\">")
                        .append(esc(lines[0].substring(0, i)))
                        .append("</td><td style=\"padding:8px 12px;font-weight:bold;color:" + NAVY + ";border-bottom:1px solid #e3e8f0;\" align=\"right\">")
                        .append(esc(lines[0].substring(i + 2))).append("</td></tr>");
                continue;
            }
            if (rows.length() > 0) {
                c.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:0 0 20px;background:#f5f8fc;border-radius:8px;border-top:3px solid " + YELLOW + ";\">")
                        .append(rows).append("</table>");
                rows.setLength(0);
            }
            if (list) {
                c.append("<ul style=\"margin:0 0 16px;padding-left:20px;\">");
                for (String l : lines) {
                    c.append("<li style=\"margin:0 0 6px;\">").append(esc(l.strip().substring(2))).append("</li>");
                }
                c.append("</ul>");
            } else if (lines.length == 1 && lines[0].strip().endsWith(":") && lines[0].length() < 70) {
                c.append("<h3 style=\"margin:24px 0 10px;font-size:16px;color:" + NAVY + ";border-left:4px solid " + YELLOW + ";padding-left:10px;\">")
                        .append(esc(lines[0].strip().replaceAll(":$", ""))).append("</h3>");
            } else {
                c.append("<p style=\"margin:0 0 16px;\">");
                for (int i = 0; i < lines.length; i++) {
                    if (i > 0) c.append("<br>");
                    c.append(inline(lines[i].strip()));
                }
                c.append("</p>");
            }
        }
        if (rows.length() > 0) {
            c.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:0 0 20px;background:#f5f8fc;border-radius:8px;border-top:3px solid " + YELLOW + ";\">")
                    .append(rows).append("</table>");
        }
        return "<!DOCTYPE html><html lang=\"cs\"><body style=\"margin:0;padding:0;background:#eef1f6;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#eef1f6;padding:24px 12px;\"><tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px;width:100%;background:#ffffff;border-radius:12px;overflow:hidden;font-family:Arial,Helvetica,sans-serif;\">"
                + "<tr><td align=\"center\" style=\"background:" + NAVY + ";padding:32px 20px 28px;border-bottom:4px solid " + YELLOW + ";\">"
                + "<img src=\"cid:logo\" alt=\"Hopík4Kids\" height=\"90\" style=\"height:90px;width:auto;display:block;border:0;\"></td></tr>"
                + "<tr><td style=\"padding:28px 32px;color:#1f2937;font-size:15px;line-height:1.6;\">" + c + "</td></tr>"
                + "<tr><td align=\"center\" style=\"background:" + NAVY + ";padding:18px;color:#cbd5e1;font-size:12px;\">"
                + "Hopík4Kids s.r.o. &middot; hopik4kids.cz</td></tr>"
                + "</table></td></tr></table></body></html>";
    }
}
