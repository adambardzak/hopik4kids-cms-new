package cz.hopik4kids.cms.registrations.repository;

import cz.hopik4kids.cms.registrations.domain.PaymentStatus;
import cz.hopik4kids.cms.registrations.domain.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RegistrationRepository extends JpaRepository<Registration, String> {

    /**
     * Admin listing with optional filters (prd §5.2b, §6.3). Fetches child+parent+program to
     * avoid N+1 and to allow personalId decryption inside the transaction. Optional case-insensitive
     * fulltext over child name, parent name and parent email.
     */
    @Query("""
            select r from Registration r
            join fetch r.child c
            join fetch c.parent pa
            join fetch r.program p
            where (:programId is null or p.id = :programId)
              and (:paymentStatus is null or r.paymentStatus = :paymentStatus)
              and (
                :q is null
                or lower(c.fullName) like lower(concat('%', cast(:q as string), '%'))
                or lower(pa.name) like lower(concat('%', cast(:q as string), '%'))
                or lower(pa.email) like lower(concat('%', cast(:q as string), '%'))
              )
            order by r.createdAt desc
            """)
    List<Registration> findForAdmin(@Param("programId") String programId,
                                    @Param("paymentStatus") PaymentStatus paymentStatus,
                                    @Param("q") String q);
}
