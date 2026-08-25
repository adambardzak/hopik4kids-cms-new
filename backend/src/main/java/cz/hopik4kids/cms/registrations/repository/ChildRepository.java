package cz.hopik4kids.cms.registrations.repository;

import cz.hopik4kids.cms.registrations.domain.Child;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChildRepository extends JpaRepository<Child, String> {
}
