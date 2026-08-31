package cz.hopik4kids.cms.records.repository;

import cz.hopik4kids.cms.records.domain.RecordDocument;
import cz.hopik4kids.cms.records.domain.RecordType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecordDocumentRepository extends JpaRepository<RecordDocument, String> {

    List<RecordDocument> findAllByOrderByDocDateDescCreatedAtDesc();

    List<RecordDocument> findByTypeOrderByDocDateDescCreatedAtDesc(RecordType type);

    List<RecordDocument> findByPersonIdOrderByDocDateDescCreatedAtDesc(String personId);
}
