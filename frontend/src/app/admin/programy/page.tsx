import { listLocations, listPrograms, listTrainers } from "@/lib/admin-data";
import { PageHeader, EmptyState } from "@/components/page-header";
import { ProgramsManager } from "./programs-manager";

export default async function ProgramyPage() {
  const [{ items: programs }, { items: locations }, { items: trainers }] = await Promise.all([
    listPrograms(),
    listLocations(),
    listTrainers(),
  ]);

  return (
    <div>
      <PageHeader title="Programy" description="Kroužky, cvičení ve školách a kempy." />
      {programs.length === 0 ? (
        <div className="space-y-4">
          <ProgramsManager programs={[]} locations={locations} trainers={trainers} openCreateOnly />
          <EmptyState message="Zatím žádné programy — přidej první kroužek nebo kemp." />
        </div>
      ) : (
        <ProgramsManager programs={programs} locations={locations} trainers={trainers} />
      )}
    </div>
  );
}
