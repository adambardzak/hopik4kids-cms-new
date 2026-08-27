package cz.hopik4kids.cms.usersrbac.repository;

import cz.hopik4kids.cms.usersrbac.domain.Role;
import cz.hopik4kids.cms.usersrbac.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByCalendarToken(String calendarToken);

    boolean existsByRole(Role role);

    long countByRole(Role role);

    java.util.List<User> findByRoleOrderByName(Role role);
}
