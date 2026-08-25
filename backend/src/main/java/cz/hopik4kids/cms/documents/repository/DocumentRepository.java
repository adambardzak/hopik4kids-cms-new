package cz.hopik4kids.cms.documents.repository;

import cz.hopik4kids.cms.documents.domain.Document;
import cz.hopik4kids.cms.documents.domain.DocumentVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, String> {

    @Query("select d from Document d left join fetch d.file order by d.category, d.sortOrder, d.title")
    List<Document> findAllOrdered();

    @Query("""
            select d from Document d left join fetch d.file
            where d.visibility = :visibility
            order by d.category, d.sortOrder, d.title
            """)
    List<Document> findByVisibilityOrdered(DocumentVisibility visibility);
}
