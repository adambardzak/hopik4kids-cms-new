import { listPrograms } from "@/lib/admin-data";
import { getSession } from "@/lib/session";
import { PageHeader, EmptyState } from "@/components/page-header";
import { AttendanceView } from "./attendance-view";

export default async function DochazkaPage() {
  const session = await getSession();
  const { items: programs } = await listPrograms();
  // Only club/school programs make sense for weekly attendance.
  const lessonPrograms = programs.filter((p) => p.type === "club" || p.type === "school");

  const isTrainer = session?.role === "trainer";

  return (
    <div>
      <PageHeader
        title="Docházka"
        description="Zaznamenej docházku dětí na lekci. Vyber program a datum."
      />
      {lessonPrograms.length === 0 ? (
        <EmptyState
          message={
            isTrainer
              ? "Nemáš přiřazené žádné kroužky. Požádej administrátora, aby tě přiřadil ke tvým programům (Nastavení → Programy → přiřadit trenéra)."
              : "Zatím žádné kroužky. Přidej program typu kroužek nebo cvičení ve škole."
          }
        />
      ) : (
        <AttendanceView programs={lessonPrograms} />
      )}
    </div>
  );
}
