package cz.hopik4kids.cms.kernel.email;

import cz.hopik4kids.cms.kernel.web.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/** Outgoing e-mail log for troubleshooting delivery. Owner/admin only. */
@RestController
@RequestMapping("/admin/api/email-log")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class EmailLogController {

    public record EmailLogDto(String id, Instant sentAt, String recipient, String subject,
                              boolean success, String error, String attachment) {
    }

    private final EmailLogRepository repo;

    public EmailLogController(EmailLogRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public PageResponse<EmailLogDto> list(@RequestParam(required = false) String q) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        return PageResponse.ofAll(repo.search(query, PageRequest.of(0, 300)).stream()
                .map(e -> new EmailLogDto(e.getId(), e.getCreatedAt(), e.getRecipient(), e.getSubject(),
                        e.isSuccess(), e.getError(), e.getAttachment()))
                .toList());
    }
}
