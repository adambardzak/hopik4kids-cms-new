package cz.hopik4kids.cms.notifications.repository;

import cz.hopik4kids.cms.notifications.domain.PushSubscription;
import cz.hopik4kids.cms.usersrbac.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, String> {

    Optional<PushSubscription> findByEndpoint(String endpoint);

    List<PushSubscription> findByUserId(String userId);

    /** All subscriptions belonging to users with any of the given roles (for targeted alerts). */
    @Query("""
            select s from PushSubscription s
            where s.userId in (select u.id from User u where u.role in :roles)
            """)
    List<PushSubscription> findByUserRoles(@Param("roles") List<Role> roles);
}
