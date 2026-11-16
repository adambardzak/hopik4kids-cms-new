package cz.hopik4kids.cms.registrations.service;

import cz.hopik4kids.cms.core.domain.Program;
import cz.hopik4kids.cms.core.domain.ProgramStatus;
import cz.hopik4kids.cms.registrations.domain.Registration;
import cz.hopik4kids.cms.registrations.domain.RegistrationStatus;
import cz.hopik4kids.cms.registrations.repository.RegistrationRepository;
import cz.hopik4kids.cms.registrations.web.dto.MediaConsentGroupDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Media (photo/video) consent per child, grouped by program — no other personal data. */
@Service
public class MediaConsentService {

    private final RegistrationRepository registrations;

    public MediaConsentService(RegistrationRepository registrations) {
        this.registrations = registrations;
    }

    @Transactional(readOnly = true)
    public List<MediaConsentGroupDto> list() {
        Collator cs = Collator.getInstance(Locale.forLanguageTag("cs"));
        Map<String, List<Registration>> byProgram = new LinkedHashMap<>();
        for (Registration r : registrations.findForAdmin(null, null, null)) {
            if (r.getStatus() == RegistrationStatus.CANCELLED
                    || r.getProgram().getStatus() == ProgramStatus.ARCHIVED) {
                continue;
            }
            byProgram.computeIfAbsent(r.getProgram().getId(), k -> new ArrayList<>()).add(r);
        }
        List<MediaConsentGroupDto> out = new ArrayList<>();
        for (List<Registration> rows : byProgram.values()) {
            Program p = rows.get(0).getProgram();
            List<MediaConsentGroupDto.Child> children = rows.stream()
                    .map(r -> new MediaConsentGroupDto.Child(r.getChild().getFullName(), r.isConsentMedia()))
                    .sorted(Comparator.comparing(MediaConsentGroupDto.Child::name, cs))
                    .toList();
            int consent = (int) children.stream().filter(MediaConsentGroupDto.Child::consentMedia).count();
            out.add(new MediaConsentGroupDto(
                    p.getId(), p.getName(),
                    p.getLocation() == null ? null : p.getLocation().getName(),
                    p.getLocation() == null ? null : p.getLocation().getAddress(),
                    consent, children));
        }
        out.sort(Comparator.comparing(MediaConsentGroupDto::programName, cs));
        return out;
    }
}
