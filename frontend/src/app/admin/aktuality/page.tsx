import { listArticles } from "@/lib/admin-data";
import { PageHeader, EmptyState } from "@/components/page-header";
import { ArticlesManager } from "./articles-manager";

export default async function AktualityPage() {
  const { items: articles } = await listArticles();

  return (
    <div>
      <PageHeader title="Aktuality" description="Novinky pro web — koncepty i publikované články." />
      {articles.length === 0 ? (
        <div className="space-y-4">
          <ArticlesManager articles={[]} openCreateOnly />
          <EmptyState message="Zatím žádné aktuality — napiš první článek." />
        </div>
      ) : (
        <ArticlesManager articles={articles} />
      )}
    </div>
  );
}
