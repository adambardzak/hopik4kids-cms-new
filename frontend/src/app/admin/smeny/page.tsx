import { listOpenShifts, listMyShifts } from "@/lib/admin-data";
import { getSession } from "@/lib/session";
import { PageHeader } from "@/components/page-header";
import { ShiftsView } from "./shifts-view";

function rangeNext6Weeks() {
  const from = new Date();
  const to = new Date();
  to.setDate(to.getDate() + 42);
  const iso = (d: Date) => d.toISOString().slice(0, 10);
  return { from: iso(from), to: iso(to) };
}

export default async function SmenyPage() {
  const { from, to } = rangeNext6Weeks();
  const [{ items: open }, { items: mine }, session] = await Promise.all([
    listOpenShifts(from, to),
    listMyShifts(from, to),
    getSession(),
  ]);
  const canApprove = session?.role === "owner" || session?.role === "admin";
  const filming = session?.role === "pr";

  return (
    <div>
      <PageHeader
        title="Směny"
        description={
          filming
            ? "Přehled všech lekcí. Přihlas se na lekci, kterou přijdeš natáčet – trenéři to uvidí."
            : "Přihlas se na volné hodiny. Admin přihlášení schvaluje."
        }
      />
      <ShiftsView open={open} mine={mine} canApprove={canApprove} filming={filming} />
    </div>
  );
}
