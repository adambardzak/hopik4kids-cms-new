import { getSession } from "@/lib/session";
import { listWorkLogs, getWorkLogSummary, listWorkLogPrograms } from "@/lib/admin-data";
import { PageHeader } from "@/components/page-header";
import { WorkLogView } from "./worklog-view";
import type { WorkLogSummary } from "@/lib/types";

/** First / last day of the current month as ISO, for the default period filter. */
function monthRange(): { from: string; to: string } {
  const now = new Date();
  const first = new Date(now.getFullYear(), now.getMonth(), 1);
  const last = new Date(now.getFullYear(), now.getMonth() + 1, 0);
  return { from: first.toISOString().slice(0, 10), to: last.toISOString().slice(0, 10) };
}

export default async function VykazyPage({
  searchParams,
}: {
  searchParams: Promise<{ from?: string; to?: string }>;
}) {
  const sp = await searchParams;
  const def = monthRange();
  const session = await getSession();
  const privileged = session?.role === "owner" || session?.role === "admin";
  // Admins start on the current month; trainers see their whole history unless they pick a period.
  const from = sp.from ?? (privileged ? def.from : undefined);
  const to = sp.to ?? (privileged ? def.to : undefined);

  const [{ items: logs }, { items: programs }] = await Promise.all([
    listWorkLogs(from, to),
    listWorkLogPrograms(),
  ]);

  let summary: WorkLogSummary[] = [];
  if (privileged) {
    summary = (await getWorkLogSummary(from, to)).items;
  }

  return (
    <div>
      <PageHeader
        title="Výkazy hodin"
        description="Evidence odpracovaných hodin. Zapiš hodiny, admin je schválí."
      />
      <WorkLogView
        logs={logs}
        summary={summary}
        programs={programs}
        privileged={privileged}
        from={from ?? ""}
        to={to ?? ""}
      />
    </div>
  );
}
