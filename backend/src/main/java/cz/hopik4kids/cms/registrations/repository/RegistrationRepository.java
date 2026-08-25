package cz.hopik4kids.cms.registrations.repository;

import cz.hopik4kids.cms.registrations.domain.PaymentStatus;
import cz.hopik4kids.cms.registrations.domain.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RegistrationRepository extends JpaRepository<Registration, String> {

    /**
     * Admin listing with optional filters (prd §5.2b). Fetches child+parent+program to avoid N+1
     * and to allow personalId decryption inside the transaction.
     */
    @Query("""
            select r from Registration r
            join fetch r.child c
            join fetch c.parent
            join fetch r.program p
            where (:programId is null or p.id = :programId)
              and (:paymentStatus is null or r.paymentStatus = :paymentStatus)
            order by r.createdAt desc
            """)
    List<Registration> findForAdmin(@Param("programId") String programId,
                                    @Param("paymentStatus") PaymentStatus paymentStatus);
}
