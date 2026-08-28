import Link from "next/link";
import { AlertTriangle, TrendingUp, Users, CalendarDays, Wallet, Plus, ShieldAlert } from "lucide-react";
import { getSession } from "@/lib/session";
import { getDashboardStats } from "@/lib/admin-data";
import { ApiRequestError } from "@/lib/api";
import { PageHeader } from "@/components/page-header";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import type { DashboardStats } from "@/lib/types";

function czk(n: number): string {
  return n.toLocaleString("cs-CZ") + " Kč";
}

export default async function DashboardPage() {
  const session = await getSession();
  const firstName = session?.name?.split(" ")[0] ?? "";

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
          <div className="flex gap-2">
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
