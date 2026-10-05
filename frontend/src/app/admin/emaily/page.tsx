import { listEmailLog } from "@/lib/admin-data";
import { PageHeader } from "@/components/page-header";
import { EmailLogView } from "./email-log-view";

export default async function EmailyPage({ searchParams }: { searchParams: Promise<{ q?: string }> }) {
  const { q } = await searchParams;
  const { items } = await listEmailLog(q);
  return (
    <div>
      <PageHeader
        title="Odeslané e-maily"
        description="Přehled posledních 300 e-mailů odeslaných systémem – komu, kdy a jestli se odeslání povedlo."
      />
      <EmailLogView items={items} q={q ?? ""} />
    </div>
  );
}
