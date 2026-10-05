package cz.hopik4kids.cms.kernel.email;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmailLogRepository extends JpaRepository<EmailLog, String> {

    @Query("select e from EmailLog e where :q is null or lower(e.recipient) like lower(concat('%', cast(:q as string), '%')) "
            + "or lower(e.subject) like lower(concat('%', cast(:q as string), '%')) order by e.createdAt desc")
    List<EmailLog> search(@Param("q") String q, Pageable pageable);
}
