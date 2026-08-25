package cz.hopik4kids.cms.registrations.repository;

import cz.hopik4kids.cms.registrations.domain.PaymentStatus;
import cz.hopik4kids.cms.registrations.domain.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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

    // --- dashboard metrics (prd §6A.1) ---

    /** Active registrations created on/after the given instant. */
    long countByStatusAndCreatedAtGreaterThanEqual(
            cz.hopik4kids.cms.registrations.domain.RegistrationStatus status, Instant since);

    long countByStatus(cz.hopik4kids.cms.registrations.domain.RegistrationStatus status);

    /** Sum of priceSnapshot over active registrations with the given payment status. */
    @Query("""
            select coalesce(sum(r.priceSnapshot), 0) from Registration r
            where r.status = cz.hopik4kids.cms.registrations.domain.RegistrationStatus.ACTIVE
              and r.paymentStatus = :paymentStatus
            """)
    long sumPriceByPaymentStatus(@Param("paymentStatus") PaymentStatus paymentStatus);

    /** Count of active registrations with the given payment status. */
    @Query("""
            select count(r) from Registration r
            where r.status = cz.hopik4kids.cms.registrations.domain.RegistrationStatus.ACTIVE
              and r.paymentStatus = :paymentStatus
            """)
    long countActiveByPaymentStatus(@Param("paymentStatus") PaymentStatus paymentStatus);

    /** Registrations with a media consent flag (active only). */
    @Query("""
            select count(r) from Registration r
            where r.status = cz.hopik4kids.cms.registrations.domain.RegistrationStatus.ACTIVE
              and r.consentMedia = :consent
            """)
    long countActiveByConsentMedia(@Param("consent") boolean consent);

    /** Active registrations of a program with child fetched (for the attendance roster). */
    @Query("""
            select r from Registration r
            join fetch r.child c
            where r.program.id = :programId
              and r.status = cz.hopik4kids.cms.registrations.domain.RegistrationStatus.ACTIVE
            order by c.fullName
            """)
    List<Registration> findActiveWithChildByProgram(@Param("programId") String programId);

    long countByProgramId(String programId);

    /** Distinct parent emails of active registrations in a program (for bulk email, prd §6A.3). */
    @Query("""
            select distinct pa.email from Registration r
            join r.child c
            join c.parent pa
            where r.program.id = :programId
              and r.status = cz.hopik4kids.cms.registrations.domain.RegistrationStatus.ACTIVE
              and pa.email is not null
            """)
    List<String> findParentEmailsByProgram(@Param("programId") String programId);

    /** All active registrations with child+parent+program fetched (marketing/retention analysis). */
    @Query("""
            select r from Registration r
            join fetch r.child c
            join fetch c.parent
            join fetch r.program p
            where r.status = cz.hopik4kids.cms.registrations.domain.RegistrationStatus.ACTIVE
            """)
    List<Registration> findAllActiveWithDetails();

    /** Source distribution over active registrations. */
    @Query("""
            select coalesce(r.source, ''), count(r) from Registration r
            where r.status = cz.hopik4kids.cms.registrations.domain.RegistrationStatus.ACTIVE
            group by coalesce(r.source, '')
            """)
    List<Object[]> countBySource();
}
