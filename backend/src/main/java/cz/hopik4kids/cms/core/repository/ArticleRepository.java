package cz.hopik4kids.cms.core.repository;

import cz.hopik4kids.cms.core.domain.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, String> {

    Optional<Article> findBySlug(String slug);

    /** All articles with cover eagerly fetched (avoids LazyInitializationException in DTO mapping), newest first. */
    @Query("select a from Article a left join fetch a.cover order by a.createdAt desc")
    List<Article> findAllWithCover();

    /** Published articles only (publishedAt not null) with cover fetched, newest first (prd §5.2). */
    @Query("select a from Article a left join fetch a.cover where a.publishedAt is not null order by a.publishedAt desc")
    List<Article> findPublished();

    /** Single article by slug with cover fetched (for public/admin detail). */
    @Query("select a from Article a left join fetch a.cover where a.slug = :slug")
    Optional<Article> findBySlugWithCover(String slug);
}
