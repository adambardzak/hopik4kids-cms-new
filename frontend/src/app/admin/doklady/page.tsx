import { listRecords, listTrainers } from "@/lib/admin-data";
import { PageHeader } from "@/components/page-header";
import { RecordsView } from "./records-view";

export default async function DokladyPage({
  searchParams,
}: {
  searchParams: Promise<{ type?: string }>;
}) {
  const sp = await searchParams;

  const [{ items: records }, { items: trainers }] = await Promise.all([
    listRecords(sp.type),
    listTrainers(),
  ]);

  return (
    <div>
      <PageHeader
        title="Doklady"
        description="Účtenky, dohody (DPP) a smlouvy. Soubory jsou uložené soukromě, přístup jen pro vedení."
      />
      <RecordsView records={records} trainers={trainers} activeType={sp.type ?? ""} />
    </div>
  );
}
