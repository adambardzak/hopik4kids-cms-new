import { listDocuments } from "@/lib/admin-data";
import { getSession } from "@/lib/session";
import { PageHeader } from "@/components/page-header";
import { DocumentsView } from "./documents-view";

export default async function DokumentyPage() {
  const [{ items: documents }, session] = await Promise.all([listDocuments(), getSession()]);
  const canEdit = session?.role === "owner" || session?.role === "admin";

  return (
    <div>
      <PageHeader
        title="Dokumenty"
        description="Hopíkovská pravidla, metodika, checklisty a formuláře — vždy po ruce."
      />
      <DocumentsView documents={documents} canEdit={canEdit} />
    </div>
  );
}
