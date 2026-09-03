import Link from "next/link";
import { AlertTriangle, TrendingUp, Users, CalendarDays, Wallet, Plus, ShieldAlert, Clock, MapPin, ClipboardCheck, ClipboardList } from "lucide-react";
import { getSession } from "@/lib/session";
import { getDashboardStats, listSchedule, listRegistrations } from "@/lib/admin-data";
import { ApiRequestError } from "@/lib/api";
import { PageHeader } from "@/components/page-header";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import type { DashboardStats, ScheduleEntry, Registration } from "@/lib/types";
import { czk, czDate } from "@/lib/format";

const WEEKDAYS = ["neděle", "pondělí", "úterý", "středa", "čtvrtek", "pátek", "sobota"];

const PAYMENT_BADGE: Record<string, { label: string; variant: "success" | "info" | "warning" | "danger" | "default" }> = {
  paid: { label: "Zaplaceno", variant: "success" },
  invoice_sent: { label: "Faktura odeslána", variant: "info" },
  unpaid: { label: "Nezaplaceno", variant: "warning" },
  cancelled: { label: "Storno", variant: "default" },
};

export default async function DashboardPage() {
  const session = await getSession();
  const firstName = session?.name?.split(" ")[0] ?? "";
  const today = new Date();
  const todayIso = today.toISOString().slice(0, 10);
  const todayLabel = `${WEEKDAYS[today.getDay()]} ${today.getDate()}. ${today.getMonth() + 1}.`;

  // Fetch stats + today's schedule in parallel (independent) to cut the dashboard load time.
  const [statsResult, scheduleResult, registrationsResult] = await Promise.allSettled([
    getDashboardStats(),
    listSchedule({ from: todayIso, to: todayIso }),
    listRegistrations(),
  ]);

  let stats: DashboardStats | null = null;
  if (statsResult.status === "fulfilled") {
    stats = statsResult.value;
  } else {
    const e = statsResult.reason;
    // 403 = trainer without dashboard access → simpler view. Other errors bubble to error.tsx.
    if (e instanceof ApiRequestError && e.status === 403) {
      stats = null;
    } else {
      throw e;
    }
  }

  // Today's lessons (best-effort; never break the dashboard if the schedule call fails).
  let todayLessons: ScheduleEntry[] = [];
  if (scheduleResult.status === "fulfilled") {
    todayLessons = scheduleResult.value.items
      .filter((e) => e.overrideType !== "cancelled")
      .sort((a, b) => a.startTime.localeCompare(b.startTime));
  }

  const occupancyPct =
    stats && stats.totalCapacity > 0
      ? Math.round((stats.totalSpotsTaken / stats.totalCapacity) * 100)
      : 0;

  // Latest registrations (best-effort; newest first).
  let recentRegistrations: Registration[] = [];
  if (registrationsResult.status === "fulfilled") {
    recentRegistrations = [...registrationsResult.value.items]
      .filter((r) => r.status !== "cancelled")
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
      .slice(0, 6);
  }

  return (
    <div>
      <PageHeader
        title={`Vítej${firstName ? ", " + firstName : ""}`}
        description="Přehled administrace Hopík4Kids."
        action={
          <div className="flex flex-wrap gap-2">
            <Button variant="outline" asChild>
              <Link href="/admin/dochazka">
                <ClipboardCheck className="h-4 w-4" /> Docházka
              </Link>
            </Button>
            <Button variant="outline" asChild>
              <Link href="/admin/aktuality">
                <Plus className="h-4 w-4" /> Aktualita
              </Link>
            </Button>
            <Button asChild>
              <Link href="/admin/programy">
                <Plus className="h-4 w-4" /> Program
              </Link>
            </Button>
          </div>
        }
      />

      {!stats ? (
        <Card>
          <CardContent className="p-6 text-sm text-[var(--muted-foreground)]">
            Vítej v administraci. V menu vlevo najdeš rozvrh a registrace svých lekcí.
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-6">
          {/* Today's lessons */}
          {todayLessons.length === 0 ? (
            <div className="flex items-center gap-3 rounded-xl border border-[var(--border)] bg-[var(--background)] px-5 py-3.5 text-sm">
              <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-[var(--accent)]/10 text-[var(--accent)]">
                <CalendarDays className="h-[18px] w-[18px]" />
              </span>
              <span className="text-[var(--muted-foreground)]">
                <span className="font-medium text-[var(--foreground)]">Dnešní lekce · {todayLabel}</span>
                {" — "}dnes nejsou naplánované žádné lekce.
              </span>
              <Link href="/admin/rozvrh" className="ml-auto shrink-0 text-sm font-medium text-[var(--accent)] hover:underline">
                Zobrazit rozvrh →
              </Link>
            </div>
          ) : (
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-base">
                  <CalendarDays className="h-4 w-4" /> Dnešní lekce
                  <span className="font-normal text-[var(--muted-foreground)]">· {todayLabel}</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <ul className="divide-y divide-[var(--border)]">
                  {todayLessons.map((l, i) => (
                    <li
                      key={`${l.programId}-${l.startTime}-${i}`}
                      className="flex flex-wrap items-center gap-x-4 gap-y-1 py-2.5 first:pt-0 last:pb-0"
                    >
                      <span className="flex items-center gap-1.5 font-semibold tabular-nums">
                        <Clock className="h-4 w-4 text-[var(--muted-foreground)]" />
                        {l.startTime}
                        {l.endTime ? `–${l.endTime}` : ""}
                      </span>
                      <span className="font-medium">{l.title ?? l.programName}</span>
                      {l.locationName && (
                        <span className="flex items-center gap-1 text-sm text-[var(--muted-foreground)]">
                          <MapPin className="h-3.5 w-3.5" />
                          {l.locationName}
                        </span>
                      )}
                      {l.overrideType === "moved" && <Badge variant="info">přesunuto</Badge>}
                      {l.overrideType === "one_off" && <Badge variant="info">jednorázově</Badge>}
                      <Button variant="ghost" size="sm" className="ml-auto" asChild>
                        <Link href="/admin/dochazka">
                          <ClipboardCheck className="h-4 w-4" /> Docházka
                        </Link>
                      </Button>
                    </li>
                  ))}
                </ul>
              </CardContent>
            </Card>
          )}

          {/* Top metrics */}
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Metric
              icon={CalendarDays}
              label="Nové registrace"
              value={String(stats.registrationsToday)}
              hint={`${stats.registrationsThisWeek} tento týden`}
              tone="brand"
              href="/admin/registrace"
            />
            <Metric
              icon={Users}
              label="Obsazenost"
              value={`${stats.totalSpotsTaken}/${stats.totalCapacity}`}
              hint={`${occupancyPct} % · ${stats.activePrograms} programů`}
              tone="brand"
              progress={occupancyPct}
              href="/admin/programy"
            />
            <Metric
              icon={Wallet}
              label="Potvrzené tržby"
              value={czk(stats.confirmedRevenue)}
              hint={`očekávané ${czk(stats.expectedRevenue)}`}
              tone="success"
              href="/admin/fakturace"
            />
            <Metric
              icon={TrendingUp}
              label="Potenciál sezóny"
              value={czk(stats.potentialRevenue)}
              hint="při plné kapacitě"
              tone="neutral"
            />
          </div>

          {/* Unpaid + underfilled */}
          <div className="grid gap-4 lg:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-base">
                  <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-[var(--warning-bg)] text-[var(--warning-fg)]">
                    <Wallet className="h-4 w-4" />
                  </span>
                  Nezaplacené registrace
                </CardTitle>
              </CardHeader>
              <CardContent>
                {stats.unpaidCount === 0 ? (
                  <p className="text-sm text-[var(--muted-foreground)]">Vše zaplaceno. 🎉</p>
                ) : (
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-2xl font-bold text-warning">{czk(stats.unpaidAmount)}</p>
                      <p className="text-sm text-[var(--muted-foreground)]">
                        {stats.unpaidCount}× čeká na platbu
                      </p>
                    </div>
                    <Button variant="outline" size="sm" asChild>
                      <Link href="/admin/registrace?paymentStatus=unpaid">Zobrazit →</Link>
                    </Button>
                  </div>
                )}
                {stats.withoutMediaConsent > 0 && (
                  <div className="mt-3 flex items-center gap-2 border-t border-[var(--border)] pt-3 text-sm text-[var(--muted-foreground)]">
                    <ShieldAlert className="h-4 w-4 text-warning" />
                    {stats.withoutMediaConsent}× bez souhlasu s fotografováním — pozor při publikaci fotek.
                  </div>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-base">
                  <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-[var(--warning-bg)] text-[var(--warning-fg)]">
                    <AlertTriangle className="h-4 w-4" />
                  </span>
                  Nenaplněné programy
                </CardTitle>
              </CardHeader>
              <CardContent>
                {stats.underfilled.length === 0 ? (
                  <p className="text-sm text-[var(--muted-foreground)]">
                    Všechny programy mají dobrou obsazenost.
                  </p>
                ) : (
                  <>
                    <ul className="space-y-2">
                      {stats.underfilled.slice(0, 5).map((p) => (
                        <li key={p.id} className="flex items-center justify-between gap-2 text-sm">
                          <Link href="/admin/programy" className="truncate font-medium hover:underline">
                            {p.name}
                          </Link>
                          <Badge variant={p.occupancyPct === 0 ? "danger" : "warning"}>
                            {p.spotsTaken}/{p.capacity} · {p.occupancyPct} %
                          </Badge>
                        </li>
                      ))}
                    </ul>
                    <div className="mt-3 border-t border-[var(--border)] pt-3 text-right">
                      <Link href="/admin/programy" className="text-sm font-medium text-[var(--accent)] hover:underline">
                        Všechny programy →
                      </Link>
                    </div>
                  </>
                )}
              </CardContent>
            </Card>
          </div>

          {/* Latest registrations */}
          {recentRegistrations.length > 0 && (
            <Card>
              <CardHeader className="flex-row items-center justify-between space-y-0">
                <CardTitle className="flex items-center gap-2 text-base">
                  <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-[var(--accent)]/10 text-[var(--accent)]">
                    <ClipboardList className="h-4 w-4" />
                  </span>
                  Poslední registrace
                </CardTitle>
                <Link href="/admin/registrace" className="text-sm font-medium text-[var(--accent)] hover:underline">
                  Všechny →
                </Link>
              </CardHeader>
              <CardContent>
                <ul className="divide-y divide-[var(--border)]">
                  {recentRegistrations.map((r) => {
                    const pay = PAYMENT_BADGE[r.paymentStatus] ?? PAYMENT_BADGE.unpaid;
                    return (
                      <li
                        key={r.id}
                        className="flex flex-wrap items-center gap-x-3 gap-y-1 py-2.5 first:pt-0 last:pb-0"
                      >
                        <span className="min-w-0 flex-1">
                          <span className="block truncate font-medium">{r.childName}</span>
                          <span className="block truncate text-xs text-[var(--muted-foreground)]">
                            {r.programName}
                          </span>
                        </span>
                        {r.priceSnapshot === 0 ? (
                          <Badge variant="default">Zdarma</Badge>
                        ) : (
                          <Badge variant={pay.variant}>{pay.label}</Badge>
                        )}
                        <span className="w-20 shrink-0 text-right text-sm text-[var(--muted-foreground)] tabular-nums">
                          {czDate(r.createdAt)}
                        </span>
                      </li>
                    );
                  })}
                </ul>
              </CardContent>
            </Card>
          )}
        </div>
      )}
    </div>
  );
}

function Metric({
  icon: Icon,
  label,
  value,
  hint,
  tone = "neutral",
  href,
  progress,
}: {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  value: string;
  hint?: string;
  tone?: "neutral" | "success" | "warning" | "brand";
  href?: string;
  progress?: number;
}) {
  const tones: Record<string, { chip: string; ring: string; bar: string; value: string }> = {
    neutral: { chip: "bg-[var(--muted)] text-[var(--foreground)]", ring: "", bar: "bg-[var(--foreground)]", value: "" },
    brand: { chip: "bg-[var(--accent)]/10 text-[var(--accent)]", ring: "", bar: "bg-[var(--accent)]", value: "" },
    success: { chip: "bg-[var(--success-bg)] text-[var(--success-fg)]", ring: "", bar: "bg-[var(--success-solid)]", value: "text-success" },
    warning: { chip: "bg-[var(--warning-bg)] text-[var(--warning-fg)]", ring: "", bar: "bg-[var(--warning-solid)]", value: "text-warning" },
  };
  const t = tones[tone];

  const inner = (
    <div className="flex h-full flex-col p-5">
      <div className="mb-3 flex items-center gap-2.5">
        <span className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg ${t.chip}`}>
          <Icon className="h-[18px] w-[18px]" />
        </span>
        <span className="text-sm font-medium text-[var(--muted-foreground)]">{label}</span>
      </div>
      <p className={`text-2xl font-bold tabular-nums ${t.value}`}>{value}</p>
      {progress != null && (
        <div className="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-[var(--muted)]">
          <div className={`h-full rounded-full ${t.bar}`} style={{ width: `${Math.min(100, progress)}%` }} />
        </div>
      )}
      {hint && <p className="mt-1.5 text-xs text-[var(--muted-foreground)]">{hint}</p>}
    </div>
  );

  return (
    <Card className={`overflow-hidden transition-shadow ${href ? "hover:shadow-md" : ""}`}>
      {href ? (
        <Link href={href} className="block h-full">
          {inner}
        </Link>
      ) : (
        inner
      )}
    </Card>
  );
}
