import { listInvoices, getSupplierSettings } from "@/lib/admin-data";
import { PageHeader } from "@/components/page-header";
import { BillingView } from "./billing-view";

export default async function FakturacePage() {
  const [{ items: invoices }, supplier] = await Promise.all([
    listInvoices(),
    getSupplierSettings(),
  ]);

  return (
    <div>
      <PageHeader title="Fakturace" description="Faktury za registrace + nastavení dodavatele a QR platby." />
      <BillingView invoices={invoices} supplier={supplier} />
    </div>
  );
}
