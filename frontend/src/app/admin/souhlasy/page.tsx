import { listMediaConsents } from "@/lib/admin-data";
import { PageHeader, EmptyState } from "@/components/page-header";
import { ConsentsView } from "./consents-view";

export default async function SouhlasyPage() {
  const { items: groups } = await listMediaConsents();

  return (
    <div>
      <PageHeader
        title="Souhlasy foto/video"
        description="Seznamy dětí podle kroužků a školek. U každého dítěte je vidět, jestli rodič souhlasil se zveřejněním fotek a videí."
      />
      {groups.length === 0 ? (
        <EmptyState message="Zatím žádné přihlášené děti." />
      ) : (
        <ConsentsView groups={groups} />
      )}
    </div>
  );
}
