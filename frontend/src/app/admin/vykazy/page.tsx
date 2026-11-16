import { getSession } from "@/lib/session";
import { listWorkLogs, listWorkLogPrograms, getMyWorkReport, listWorkReports } from "@/lib/admin-data";
import { PageHeader } from "@/components/page-header";
import { WorkLogView } from "./worklog-view";
import type { Program, WorkLog, WorkReport } from "@/lib/types";

/** Current month as YYYY-MM. */
function currentMonth(): string {
  // Server runs in UTC; the business month is Prague's.
  return new Intl.DateTimeFormat("sv-SE", { timeZone: "Europe/Prague" }).format(new Date()).slice(0, 7);
}

/** First / last day of a YYYY-MM month as ISO dates. */
function monthRange(month: string): { from: string; to: string } {
  const [y, m] = month.split("-").map(Number);
  const last = new Date(Date.UTC(y, m, 0)).getUTCDate();
  return { from: `${month}-01`, to: `${month}-${String(last).padStart(2, "0")}` };
}

export default async function VykazyPage({
  searchParams,
}: {
  searchParams: Promise<{ month?: string }>;
}) {
  const sp = await searchParams;
  const month = sp.month && /^\d{4}-\d{2}$/.test(sp.month) ? sp.month : currentMonth();
  const { from, to } = monthRange(month);

  const session = await getSession();
  const role = session?.role;
  const privileged = role === "owner" || role === "admin";
  // The accountant only sees the team's monthly reports (no individual entries).
  const accountant = role === "accountant";
  const worksShifts = !accountant;

  const [logsRes, programsRes, myReport, reportsRes] = await Promise.all([
    worksShifts ? listWorkLogs(from, to) : Promise.resolve({ items: [] as WorkLog[] }),
    worksShifts ? listWorkLogPrograms() : Promise.resolve({ items: [] as Program[] }),
    worksShifts ? getMyWorkReport(month) : Promise.resolve(null),
    privileged || accountant ? listWorkReports(month) : Promise.resolve({ items: [] as WorkReport[] }),
  ]);

  return (
    <div>
      <PageHeader
        title="Výkazy práce"
        description={
          accountant
            ? "Schválené měsíční výkazy brigádníků. Stáhni je jednotlivě, nebo všechny najednou."
            : "Hodiny se zapisují samy z potvrzených směn. Na konci měsíce je zkontroluj, případně oprav, a odešli výkaz ke schválení."
        }
      />
      <WorkLogView
        month={month}
        logs={logsRes.items}
        programs={programsRes.items}
        myReport={myReport}
        reports={reportsRes.items}
        privileged={privileged}
        accountant={accountant}
        currentUserId={session?.userId ?? ""}
      />
    </div>
  );
}
