package cz.hopik4kids.cms.usersrbac.repository;

import cz.hopik4kids.cms.usersrbac.domain.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, String> {

    Optional<Invitation> findByTokenHash(String tokenHash);
}
