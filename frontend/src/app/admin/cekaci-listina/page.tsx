import { listPrograms } from "@/lib/admin-data";
import { PageHeader } from "@/components/page-header";
import { WaitlistBulkView } from "./waitlist-bulk-view";

/** Per-program operational tools: waitlist management + bulk email (prd §6A.2, §6A.3). */
export default async function CekaciListinaPage() {
  const { items: programs } = await listPrograms();

  return (
    <div>
      <PageHeader
        title="Čekací listina & rozesílání"
        description="Správa zájemců u naplněných programů a hromadný e-mail účastníkům."
      />
      <WaitlistBulkView programs={programs} />
    </div>
  );
}
