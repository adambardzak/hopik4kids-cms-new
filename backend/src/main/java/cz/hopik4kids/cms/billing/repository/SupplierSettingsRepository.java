package cz.hopik4kids.cms.billing.repository;

import cz.hopik4kids.cms.billing.domain.SupplierSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierSettingsRepository extends JpaRepository<SupplierSettings, String> {
}
