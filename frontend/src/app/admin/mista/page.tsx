import { listLocations } from "@/lib/admin-data";
import { PageHeader, EmptyState } from "@/components/page-header";
import { LocationsManager } from "./locations-manager";

export default async function MistaPage() {
  const { items: locations } = await listLocations();

  return (
    <div>
      <PageHeader title="Místa" description="Školky, tělocvičny a sportoviště — sdílená napříč programy." />
      {locations.length === 0 ? (
        <div className="space-y-4">
          <LocationsManager locations={[]} openCreateOnly />
          <EmptyState message="Zatím žádná místa — přidej první školku nebo sportoviště." />
        </div>
      ) : (
        <LocationsManager locations={locations} />
      )}
    </div>
  );
}
