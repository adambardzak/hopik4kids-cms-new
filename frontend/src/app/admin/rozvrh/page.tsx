import { listLocations, listSchedule, listPrograms } from "@/lib/admin-data";
import { PageHeader } from "@/components/page-header";
import { getSession } from "@/lib/session";
import { ScheduleView } from "./schedule-view";

/** Monday (ISO) of the week containing the given date. */
function mondayOf(date: Date): Date {
  const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
  const day = d.getUTCDay() || 7; // Sun=0 -> 7
  if (day !== 1) d.setUTCDate(d.getUTCDate() - (day - 1));
  return d;
}

function iso(d: Date): string {
  return d.toISOString().slice(0, 10);
}

export default async function RozvrhPage({
  searchParams,
}: {
  searchParams: Promise<{ week?: string; location?: string }>;
}) {
  const sp = await searchParams;

  const base = sp.week ? new Date(sp.week + "T00:00:00Z") : new Date();
  const monday = mondayOf(base);
  const sunday = new Date(monday);
  sunday.setUTCDate(sunday.getUTCDate() + 6);

  const from = iso(monday);
  const to = iso(sunday);

  const session = await getSession();
  // PR manager: read-only schedule; program admin data (prices etc.) isn't available to them.
  const readOnly = session?.role === "pr";
  const [{ items: entries }, { items: locations }, { items: programs }] = await Promise.all([
    listSchedule({ from, to, location: sp.location }),
    listLocations(),
    readOnly ? Promise.resolve({ items: [] }) : listPrograms(),
  ]);

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <PageHeader
        title="Rozvrh"
        description="Týdenní přehled lekcí. Prázdná okna = volné termíny, které můžete nabídnout školce."
      />
      <div className="min-h-0 flex-1">
        <ScheduleView
          entries={entries}
          locations={locations}
          programs={programs}
          weekStart={from}
          currentLocation={sp.location ?? ""}
          readOnly={readOnly}
        />
      </div>
    </div>
  );
}
