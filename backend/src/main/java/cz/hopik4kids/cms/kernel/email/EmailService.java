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

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.email.from:no-reply@hopik4kids.cz}") String from,
                        @Value("${app.admin-base-url:http://localhost:3000}") String appBaseUrl) {
        this.mailSender = mailSender;
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
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            return true;
        } catch (MailException e) {
            log.error("Failed to send email '{}' to {}: {}", subject, to, e.getMessage());
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
            helper.setText(body, false);
            helper.addAttachment(attachmentName, new ByteArrayResource(attachment), contentType);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("Failed to send email with attachment '{}' to {}: {}", subject, to, e.getMessage());
            return false;
        }
    }
}

