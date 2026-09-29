import { listInvoices, getSupplierSettings, listCreditNotes } from "@/lib/admin-data";
import { PageHeader } from "@/components/page-header";
import { BillingView } from "./billing-view";

export default async function FakturacePage({
  searchParams,
}: {
  searchParams: Promise<{ from?: string; to?: string; status?: string; type?: string }>;
}) {
  const sp = await searchParams;
  const filters = { from: sp.from, to: sp.to, status: sp.status, type: sp.type };

  const [{ items: invoices }, supplier, { items: creditNotes }] = await Promise.all([
    listInvoices(filters),
    getSupplierSettings(),
    listCreditNotes(),
  ]);

  return (
    <div>
      <PageHeader title="Fakturace" description="Faktury za registrace + nastavení dodavatele a QR platby." />
      <BillingView invoices={invoices} supplier={supplier} filters={filters} creditNotes={creditNotes} />
    </div>
  );
}
