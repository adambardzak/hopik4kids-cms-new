import { PageHeader } from "@/components/page-header";
import { BankImportView } from "./bank-import-view";

export default function ParovaniPage() {
  return (
    <div>
      <PageHeader
        title="Párování plateb"
        description="Nahraj výpis z banky (CSV z Raiffeisenu). Systém navrhne spárování plateb s fakturami podle variabilního symbolu a částky."
      />
      <BankImportView />
    </div>
  );
}
