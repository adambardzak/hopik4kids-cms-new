import { listPrograms } from "@/lib/admin-data";
import { PageHeader, EmptyState } from "@/components/page-header";
import { AttendanceView } from "./attendance-view";

export default async function DochazkaPage() {
  const { items: programs } = await listPrograms();
  // Only club/school programs make sense for weekly attendance.
  const lessonPrograms = programs.filter((p) => p.type === "club" || p.type === "school");

  return (
    <div>
      <PageHeader
        title="Docházka"
        description="Zaznamenej docházku dětí na lekci. Vyber program a datum."
      />
      {lessonPrograms.length === 0 ? (
        <EmptyState message="Zatím žádné kroužky. Přidej program typu kroužek nebo cvičení ve škole." />
      ) : (
        <AttendanceView programs={lessonPrograms} />
      )}
    </div>
  );
}
