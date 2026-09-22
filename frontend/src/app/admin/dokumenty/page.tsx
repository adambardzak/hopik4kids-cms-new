import { listDocuments } from "@/lib/admin-data";
import { getSession } from "@/lib/session";
import { PageHeader } from "@/components/page-header";
import { DocumentsView } from "./documents-view";

export default async function DokumentyPage() {
  const [{ items: documents }, session] = await Promise.all([listDocuments(), getSession()]);
  const isPrivileged = session?.role === "owner" || session?.role === "admin";
  // Trainers may write documents too, but only TRAINERS-visible ones (prd RBAC).
  const canEdit = isPrivileged || session?.role === "trainer";

  return (
    <div>
      <PageHeader
        title="Dokumenty"
        description="Hopíkovská pravidla, metodika, checklisty a formuláře — vždy po ruce."
      />
      <DocumentsView documents={documents} canEdit={canEdit} canSetAdminVisibility={isPrivileged} />
    </div>
  );
}
