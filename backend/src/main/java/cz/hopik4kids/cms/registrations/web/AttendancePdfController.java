package cz.hopik4kids.cms.registrations.web;

import cz.hopik4kids.cms.registrations.service.AttendancePdfService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Attendance / contact list PDF per program (prd §6A.3). */
@RestController
@RequestMapping("/admin/api/programs")
@PreAuthorize("hasAnyRole('OWNER','ADMIN','TRAINER')")
public class AttendancePdfController {

    private final AttendancePdfService pdf;

    public AttendancePdfController(AttendancePdfService pdf) {
        this.pdf = pdf;
    }

    @GetMapping("/{id}/attendance.pdf")
    public ResponseEntity<byte[]> attendance(@PathVariable String id) {
        byte[] body = pdf.build(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("dochazka.pdf").build().toString())
                .body(body);
    }
}
