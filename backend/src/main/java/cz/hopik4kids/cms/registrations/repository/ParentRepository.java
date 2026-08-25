package cz.hopik4kids.cms.registrations.repository;

import cz.hopik4kids.cms.registrations.domain.Parent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParentRepository extends JpaRepository<Parent, String> {
}
