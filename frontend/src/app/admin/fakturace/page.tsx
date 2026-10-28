import {
  listInvoices,
  getSupplierSettings,
  listCreditNotes,
  listSchoolContracts,
  listSchoolContractCandidates,
  listLocations,
} from "@/lib/admin-data";
import { PageHeader } from "@/components/page-header";
import { BillingView } from "./billing-view";

export default async function FakturacePage({
  searchParams,
}: {
  searchParams: Promise<{ from?: string; to?: string; status?: string; type?: string }>;
}) {
  const sp = await searchParams;
  const filters = { from: sp.from, to: sp.to, status: sp.status, type: sp.type };

  const [{ items: invoices }, supplier, { items: creditNotes }, schoolContracts, schoolCandidates, locations] =
    await Promise.all([
      listInvoices(filters),
      getSupplierSettings(),
      listCreditNotes(),
      listSchoolContracts(),
      listSchoolContractCandidates(),
      listLocations(),
    ]);

  return (
    <div>
      <PageHeader
        title="Fakturace"
        description="Faktury rodičům i školkám, dobropisy a nastavení dodavatele."
      />
      <BillingView
        invoices={invoices}
        supplier={supplier}
        filters={filters}
        creditNotes={creditNotes}
        schoolContracts={schoolContracts.items}
        schoolCandidates={schoolCandidates.items}
        locations={locations.items}
      />
    </div>
  );
}
