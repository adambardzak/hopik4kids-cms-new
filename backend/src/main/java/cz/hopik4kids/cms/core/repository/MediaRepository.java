package cz.hopik4kids.cms.core.repository;

import cz.hopik4kids.cms.core.domain.Media;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<Media, String> {
}
