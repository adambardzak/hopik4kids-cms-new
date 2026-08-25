package cz.hopik4kids.cms.core.repository;

import cz.hopik4kids.cms.core.domain.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, String> {

    Optional<Article> findBySlug(String slug);

    /** Published articles only (publishedAt not null), newest first (prd §5.2). */
    @Query("select a from Article a where a.publishedAt is not null order by a.publishedAt desc")
    List<Article> findPublished();
}
