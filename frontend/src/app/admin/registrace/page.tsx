import { listPrograms, listRegistrations } from "@/lib/admin-data";
import { PageHeader, EmptyState } from "@/components/page-header";
import { RegistrationsTable } from "./registrations-table";

export default async function RegistracePage({
  searchParams,
}: {
  searchParams: Promise<{ program?: string; paymentStatus?: string }>;
}) {
  const sp = await searchParams;
  const [{ items: registrations }, { items: programs }] = await Promise.all([
    listRegistrations({ program: sp.program, paymentStatus: sp.paymentStatus }),
    listPrograms(),
  ]);

  const exportQuery = new URLSearchParams();
  if (sp.program) exportQuery.set("program", sp.program);
  if (sp.paymentStatus) exportQuery.set("paymentStatus", sp.paymentStatus);

  return (
    <div>
      <PageHeader
        title="Registrace"
        description="Přihlášky dětí do kroužků a kempů. Detail obsahuje všechna data pro fakturaci a docházku."
      />
      {registrations.length === 0 && !sp.program && !sp.paymentStatus ? (
        <EmptyState message="Zatím žádné registrace — až se někdo přihlásí, objeví se tady." />
      ) : (
        <RegistrationsTable
          registrations={registrations}
          programs={programs}
          filters={{ program: sp.program ?? "", paymentStatus: sp.paymentStatus ?? "" }}
          exportQuery={exportQuery.toString()}
        />
      )}
    </div>
  );
}
