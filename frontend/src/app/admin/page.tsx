import Link from "next/link";
import { AlertTriangle, TrendingUp, Users, CalendarDays, Wallet, Plus, ShieldAlert, Clock, MapPin, ClipboardCheck } from "lucide-react";
import { getSession } from "@/lib/session";
import { getDashboardStats, listSchedule } from "@/lib/admin-data";
import { ApiRequestError } from "@/lib/api";
import { PageHeader } from "@/components/page-header";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import type { DashboardStats, ScheduleEntry } from "@/lib/types";
import { czk } from "@/lib/format";

const WEEKDAYS = ["neděle", "pondělí", "úterý", "středa", "čtvrtek", "pátek", "sobota"];

export default async function DashboardPage() {
  const session = await getSession();
  const firstName = session?.name?.split(" ")[0] ?? "";
  const today = new Date();
  const todayIso = today.toISOString().slice(0, 10);
  const todayLabel = `${WEEKDAYS[today.getDay()]} ${today.getDate()}. ${today.getMonth() + 1}.`;

  let stats: DashboardStats | null = null;
  try {
    stats = await getDashboardStats();
  } catch (e) {
    // 403 = trainer without dashboard access → simpler view. Other errors bubble to error.tsx.
    if (e instanceof ApiRequestError && e.status === 403) {
      stats = null;
    } else {
      throw e;
    }
  }

  // Today's lessons (best-effort; never break the dashboard if the schedule call fails).
  let todayLessons: ScheduleEntry[] = [];
  try {
    const res = await listSchedule({ from: todayIso, to: todayIso });
    todayLessons = res.items
      .filter((e) => e.overrideType !== "cancelled")
      .sort((a, b) => a.startTime.localeCompare(b.startTime));
  } catch {
    todayLessons = [];
  }

  const occupancyPct =
    stats && stats.totalCapacity > 0
      ? Math.round((stats.totalSpotsTaken / stats.totalCapacity) * 100)
      : 0;

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
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-base">
                <CalendarDays className="h-4 w-4" /> Dnešní lekce
                <span className="font-normal text-[var(--muted-foreground)]">· {todayLabel}</span>
              </CardTitle>
            </CardHeader>
            <CardContent>
              {todayLessons.length === 0 ? (
                <p className="text-sm text-[var(--muted-foreground)]">
                  Dnes nejsou naplánované žádné lekce.{" "}
                  <Link href="/admin/rozvrh" className="text-[var(--primary)] hover:underline">
                    Zobrazit rozvrh
                  </Link>
                  .
                </p>
              ) : (
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
              )}
            </CardContent>
          </Card>

          {/* Top metrics */}
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Metric
              icon={CalendarDays}
              label="Nové registrace"
              value={String(stats.registrationsToday)}
              hint={`${stats.registrationsThisWeek} tento týden`}
            />
            <Metric
              icon={Users}
              label="Obsazenost"
              value={`${stats.totalSpotsTaken}/${stats.totalCapacity}`}
              hint={`${occupancyPct} % · ${stats.activePrograms} programů`}
            />
            <Metric
              icon={Wallet}
              label="Potvrzené tržby"
              value={czk(stats.confirmedRevenue)}
              hint={`očekávané ${czk(stats.expectedRevenue)}`}
              tone="success"
            />
            <Metric
              icon={TrendingUp}
              label="Potenciál sezóny"
              value={czk(stats.potentialRevenue)}
              hint="při plné kapacitě"
            />
          </div>

          {/* Unpaid + underfilled */}
          <div className="grid gap-4 lg:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-base">
                  <Wallet className="h-4 w-4" /> Nezaplacené registrace
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
                      <Link href="/admin/registrace?paymentStatus=unpaid">Zobrazit</Link>
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
                  <AlertTriangle className="h-4 w-4 text-warning" /> Nenaplněné programy
                </CardTitle>
              </CardHeader>
              <CardContent>
                {stats.underfilled.length === 0 ? (
                  <p className="text-sm text-[var(--muted-foreground)]">
                    Všechny programy mají dobrou obsazenost.
                  </p>
                ) : (
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
                )}
              </CardContent>
            </Card>
          </div>
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
  tone,
}: {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  value: string;
  hint?: string;
  tone?: "success";
}) {
  return (
    <Card>
      <CardContent className="p-5">
        <div className="mb-2 flex items-center gap-2 text-sm text-[var(--muted-foreground)]">
          <Icon className="h-4 w-4" />
          {label}
        </div>
        <p className={`text-2xl font-bold ${tone === "success" ? "text-success" : ""}`}>{value}</p>
        {hint && <p className="mt-1 text-xs text-[var(--muted-foreground)]">{hint}</p>}
      </CardContent>
    </Card>
  );
}
