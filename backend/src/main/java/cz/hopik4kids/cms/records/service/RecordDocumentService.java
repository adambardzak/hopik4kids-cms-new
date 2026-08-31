package cz.hopik4kids.cms.records.service;

import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.records.domain.RecordDocument;
import cz.hopik4kids.cms.records.domain.RecordType;
import cz.hopik4kids.cms.records.repository.RecordDocumentRepository;
import cz.hopik4kids.cms.records.web.dto.RecordDocumentDto;
import cz.hopik4kids.cms.usersrbac.domain.User;
import cz.hopik4kids.cms.usersrbac.repository.UserRepository;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Accounting/HR document records (prd todo #8): upload, list, download, delete. */
@Service
public class RecordDocumentService {

    private final RecordDocumentRepository records;
    private final RecordStorageService storage;
    private final UserRepository users;
    private final AuditService audit;

    public RecordDocumentService(RecordDocumentRepository records, RecordStorageService storage,
                                 UserRepository users, AuditService audit) {
        this.records = records;
        this.storage = storage;
        this.users = users;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<RecordDocumentDto> list(String type, String personId) {
        List<RecordDocument> rows;
        if (type != null && !type.isBlank()) {
            rows = records.findByTypeOrderByDocDateDescCreatedAtDesc(parseType(type));
        } else if (personId != null && !personId.isBlank()) {
            rows = records.findByPersonIdOrderByDocDateDescCreatedAtDesc(personId);
        } else {
            rows = records.findAllByOrderByDocDateDescCreatedAtDesc();
        }
        return rows.stream().map(this::toDto).toList();
    }

    @Transactional
    public RecordDocumentDto create(MultipartFile file, String type, String title, String personId,
                                    String personName, LocalDate docDate, BigDecimal amount, String note) {
        RecordStorageService.Stored stored = storage.store(file);

        RecordDocument r = new RecordDocument();
        r.setType(parseType(type));
        r.setTitle(title != null && !title.isBlank() ? title : originalOrDefault(file));
        r.setPersonId(blankToNull(personId));
        r.setPersonName(blankToNull(personName));
        r.setDocDate(docDate);
        r.setAmount(amount);
        r.setNote(blankToNull(note));
        r.setStoredName(stored.storedName());
        r.setOriginalName(file.getOriginalFilename());
        r.setContentType(stored.contentType());
        r.setSizeBytes(stored.size());
        r = records.save(r);

        audit.record("record.upload", "RecordDocument", r.getId(), r.getType().name());
        return toDto(r);
    }

    @Transactional(readOnly = true)
    public RecordDocument get(String id) {
        return records.findById(id).orElseThrow(() -> ApiException.notFound("Doklad nenalezen"));
    }

    public byte[] file(RecordDocument r) {
        return storage.read(r.getStoredName());
    }

    @Transactional
    public void delete(String id) {
        RecordDocument r = get(id);
        storage.delete(r.getStoredName());
        records.delete(r);
        audit.record("record.delete", "RecordDocument", id);
    }

    // --- helpers ---

    private RecordDocumentDto toDto(RecordDocument r) {
        String personName = r.getPersonName();
        if (personName == null && r.getPersonId() != null) {
            personName = users.findById(r.getPersonId()).map(User::getName).orElse(null);
        }
        return RecordDocumentDto.from(r, personName);
    }

    private static RecordType parseType(String type) {
        if (type == null || type.isBlank()) {
            return RecordType.OTHER;
        }
        try {
            return RecordType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("INVALID_TYPE", "Neplatný typ dokladu");
        }
    }

    private static String originalOrDefault(MultipartFile file) {
        String name = file.getOriginalFilename();
        return (name != null && !name.isBlank()) ? name : "Doklad";
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
