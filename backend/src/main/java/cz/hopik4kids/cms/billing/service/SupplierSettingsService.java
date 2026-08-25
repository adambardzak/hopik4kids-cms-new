package cz.hopik4kids.cms.billing.service;

import cz.hopik4kids.cms.billing.domain.SupplierSettings;
import cz.hopik4kids.cms.billing.repository.SupplierSettingsRepository;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Singleton supplier (invoicing) settings (prd §6A.5). */
@Service
public class SupplierSettingsService {

    private final SupplierSettingsRepository repo;
    private final AuditService audit;

    public SupplierSettingsService(SupplierSettingsRepository repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    /** Returns the settings row, or a transient default (name "Hopík4Kids") if not set yet. */
    @Transactional(readOnly = true)
    public SupplierSettings getOrDefault() {
        return repo.findAll().stream().findFirst().orElseGet(() -> {
            SupplierSettings s = new SupplierSettings();
            s.setName("Hopík4Kids");
            s.setDefaultDueDays(14);
            return s;
        });
    }

    @Transactional
    public SupplierSettings save(SupplierSettings incoming) {
        SupplierSettings s = repo.findAll().stream().findFirst().orElseGet(SupplierSettings::new);
        s.setName(incoming.getName());
        s.setIco(incoming.getIco());
        s.setDic(incoming.getDic());
        s.setAddress(incoming.getAddress());
        s.setIban(incoming.getIban());
        s.setAccountNumber(incoming.getAccountNumber());
        s.setWeb(incoming.getWeb());
        s.setEmail(incoming.getEmail());
        s.setDefaultDueDays(incoming.getDefaultDueDays() > 0 ? incoming.getDefaultDueDays() : 14);
        s.setFooterText(incoming.getFooterText());
        s = repo.save(s);
        audit.record("supplier-settings-save", "SupplierSettings", s.getId());
        return s;
    }
}
