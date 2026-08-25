package cz.hopik4kids.cms.core.repository;

import cz.hopik4kids.cms.core.domain.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, String> {
}
