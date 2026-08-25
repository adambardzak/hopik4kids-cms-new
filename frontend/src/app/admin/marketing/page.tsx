import Link from "next/link";
import { TrendingUp, Users, Repeat, Megaphone } from "lucide-react";
import { getMarketingStats } from "@/lib/admin-data";
import { PageHeader } from "@/components/page-header";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

export default async function MarketingPage() {
  const stats = await getMarketingStats();

  const totalSourced = stats.sources.reduce((s, x) => s + x.count, 0);

  return (
    <div>
      <PageHeader
        title="Marketing & růst"
        description="Odkud přicházejí děti, kolik se jich vrací a komu nabídnout kemp."
      />

      <div className="mb-6 grid gap-4 sm:grid-cols-3">
        <Metric icon={Repeat} label="Retence" value={`${stats.retentionPct} %`} hint={`${stats.returningChildren} z ${stats.totalDistinctChildren} dětí ve víc programech`} />
        <Metric icon={Users} label="Dětí na kempu" value={String(stats.campChildren)} />
        <Metric icon={Megaphone} label="Cross-sell příležitosti" value={String(stats.clubsNotInCamp.length)} hint="dětí z kroužků bez kempu" />
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        {/* Sources */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <TrendingUp className="h-4 w-4" /> Odkud se dozvěděli
            </CardTitle>
          </CardHeader>
          <CardContent>
            {stats.sources.length === 0 ? (
              <p className="text-sm text-[var(--muted-foreground)]">
                Zatím žádná data o zdroji. Rodiče vyplní „Odkud jste se dozvěděli" při registraci.
              </p>
            ) : (
              <div className="space-y-2">
                {stats.sources.map((s) => {
                  const pct = totalSourced > 0 ? Math.round((s.count / totalSourced) * 100) : 0;
                  return (
                    <div key={s.source}>
                      <div className="flex justify-between text-sm">
                        <span className="font-medium">{s.source}</span>
                        <span className="text-[var(--muted-foreground)]">
                          {s.count}× · {pct} %
                        </span>
                      </div>
                      <div className="mt-1 h-2 overflow-hidden rounded-full bg-[var(--muted)]">
                        <div className="h-full rounded-full bg-[var(--primary)]" style={{ width: `${pct}%` }} />
                      </div>
                    </div>
                  );
                })}
                {stats.registrationsWithoutSource > 0 && (
                  <p className="pt-2 text-xs text-[var(--muted-foreground)]">
                    {stats.registrationsWithoutSource} registrací bez uvedeného zdroje.
                  </p>
                )}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Cross-sell */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <Megaphone className="h-4 w-4" /> Nabídnout kemp (děti z kroužků)
            </CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            {stats.clubsNotInCamp.length === 0 ? (
              <p className="p-6 text-sm text-[var(--muted-foreground)]">
                Všechny děti z kroužků už jsou i na kempu. 🎉
              </p>
            ) : (
              <div className="max-h-96 overflow-auto">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Dítě</TableHead>
                      <TableHead>Rodič</TableHead>
                      <TableHead></TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {stats.clubsNotInCamp.map((c, i) => (
                      <TableRow key={c.childName + i}>
                        <TableCell className="font-medium">{c.childName}</TableCell>
                        <TableCell className="text-sm">
                          <div>{c.parentName}</div>
                          <div className="text-xs text-[var(--muted-foreground)]">{c.parentPhone}</div>
                        </TableCell>
                        <TableCell className="text-right">
                          <Button variant="ghost" size="sm" asChild>
                            <a href={`mailto:${c.parentEmail}`}>E-mail</a>
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function Metric({
  icon: Icon,
  label,
  value,
  hint,
}: {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  value: string;
  hint?: string;
}) {
  return (
    <Card>
      <CardContent className="p-5">
        <div className="mb-2 flex items-center gap-2 text-sm text-[var(--muted-foreground)]">
          <Icon className="h-4 w-4" />
          {label}
        </div>
        <p className="text-2xl font-bold">{value}</p>
        {hint && <p className="mt-1 text-xs text-[var(--muted-foreground)]">{hint}</p>}
      </CardContent>
    </Card>
  );
}
