import { getSession } from "@/lib/session";
import { PageHeader } from "@/components/page-header";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default async function DashboardPage() {
  const session = await getSession();
  const firstName = session?.name?.split(" ")[0] ?? "";

  return (
    <div>
      <PageHeader title={`Vítej${firstName ? ", " + firstName : ""}`} description="Přehled administrace Hopík4Kids." />
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <Card>
          <CardHeader>
            <CardTitle>Registrace</CardTitle>
          </CardHeader>
          <CardContent className="text-sm text-[var(--muted-foreground)]">
            Přehled a export přihlášek dětí do kroužků a kempů.
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>Programy</CardTitle>
          </CardHeader>
          <CardContent className="text-sm text-[var(--muted-foreground)]">
            Kroužky, cvičení ve školách a kempy — kapacita a ceny.
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>Aktuality</CardTitle>
          </CardHeader>
          <CardContent className="text-sm text-[var(--muted-foreground)]">
            Novinky pro web — koncepty i publikované články.
          </CardContent>
        </Card>
      </div>
      <p className="mt-8 text-sm text-[var(--muted-foreground)]">
        Detailní metriky (obsazenost, tržby, predikce) přibudou ve fázi 1.
      </p>
    </div>
  );
}
