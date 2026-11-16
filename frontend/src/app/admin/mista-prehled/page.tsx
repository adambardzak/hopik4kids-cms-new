import { MapPin } from "lucide-react";
import { listLocations } from "@/lib/admin-data";
import { PageHeader, EmptyState } from "@/components/page-header";

/** Read-only list of venues with addresses (PR manager). */
export default async function MistaPrehledPage() {
  const { items: locations } = await listLocations();
  const sorted = [...locations].sort((a, b) => a.name.localeCompare(b.name, "cs"));

  return (
    <div>
      <PageHeader title="Místa konání" description="Školky, tělocvičny a sportoviště, kde probíhají lekce." />
      {sorted.length === 0 ? (
        <EmptyState message="Zatím žádná místa." />
      ) : (
        <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {sorted.map((l) => (
            <li key={l.id} className="rounded-lg border border-[var(--border)] bg-[var(--background)] p-4">
              <p className="font-medium">{l.name}</p>
              {l.address ? (
                <a
                  href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(l.address)}`}
                  target="_blank"
                  rel="noreferrer"
                  className="mt-1 flex items-start gap-1.5 text-sm text-[var(--muted-foreground)] hover:underline"
                >
                  <MapPin className="mt-0.5 h-3.5 w-3.5 shrink-0" /> {l.address}
                </a>
              ) : (
                <p className="mt-1 text-sm text-[var(--muted-foreground)]">Adresa není vyplněná.</p>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
