"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { Download } from "lucide-react";
import type { Program, Registration } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { cancelRegistration, setPaymentStatus } from "@/lib/actions";

const PAYMENT_LABELS: Record<string, { label: string; variant: "success" | "warning" | "danger" }> = {
  paid: { label: "Zaplaceno", variant: "success" },
  unpaid: { label: "Nezaplaceno", variant: "warning" },
  cancelled: { label: "Storno", variant: "danger" },
};

export function RegistrationsTable({
  registrations,
  programs,
  filters,
  exportQuery,
}: {
  registrations: Registration[];
  programs: Program[];
  filters: { program: string; paymentStatus: string };
  exportQuery: string;
}) {
  const router = useRouter();
  const [detail, setDetail] = useState<Registration | null>(null);
  const [isPending, startTransition] = useTransition();

  function updateFilter(key: string, value: string) {
    const q = new URLSearchParams();
    const next = { ...filters, [key]: value };
    if (next.program) q.set("program", next.program);
    if (next.paymentStatus) q.set("paymentStatus", next.paymentStatus);
    router.push(`/admin/registrace${q.toString() ? `?${q}` : ""}`);
  }

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <select
          className="h-10 rounded-md border border-[var(--border)] bg-transparent px-3 text-sm"
          value={filters.program}
          onChange={(e) => updateFilter("program", e.target.value)}
        >
          <option value="">Všechny programy</option>
          {programs.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
        </select>
        <select
          className="h-10 rounded-md border border-[var(--border)] bg-transparent px-3 text-sm"
          value={filters.paymentStatus}
          onChange={(e) => updateFilter("paymentStatus", e.target.value)}
        >
          <option value="">Všechny platby</option>
          <option value="unpaid">Nezaplaceno</option>
          <option value="paid">Zaplaceno</option>
          <option value="cancelled">Storno</option>
        </select>
        <div className="ml-auto flex gap-2">
          <Button variant="outline" size="sm" asChild>
            <a href={`/api/registrations/export?format=csv&${exportQuery}`}>
              <Download className="h-4 w-4" /> CSV
            </a>
          </Button>
          <Button variant="outline" size="sm" asChild>
            <a href={`/api/registrations/export?format=xlsx&${exportQuery}`}>
              <Download className="h-4 w-4" /> Excel
            </a>
          </Button>
        </div>
      </div>

      <div className="rounded-lg border border-[var(--border)] bg-[var(--background)]">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Dítě</TableHead>
              <TableHead>Rodič</TableHead>
              <TableHead>Program</TableHead>
              <TableHead>Dres</TableHead>
              <TableHead>Platba</TableHead>
              <TableHead>Přihlášeno</TableHead>
              <TableHead></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {registrations.map((r) => {
              const pay = PAYMENT_LABELS[r.paymentStatus] ?? PAYMENT_LABELS.unpaid;
              return (
                <TableRow key={r.id} className={r.status === "cancelled" ? "opacity-50" : ""}>
                  <TableCell className="font-medium">{r.childName}</TableCell>
                  <TableCell>
                    <div>{r.parentName}</div>
                    <div className="text-xs text-[var(--muted-foreground)]">{r.parentPhone}</div>
                  </TableCell>
                  <TableCell>{r.programName}</TableCell>
                  <TableCell>{r.wantsShirt ? r.shirtSize ?? "ano" : "—"}</TableCell>
                  <TableCell>
                    <Badge variant={pay.variant}>{pay.label}</Badge>
                  </TableCell>
                  <TableCell className="text-sm text-[var(--muted-foreground)]">
                    {new Date(r.createdAt).toLocaleDateString("cs-CZ")}
                  </TableCell>
                  <TableCell>
                    <Button variant="ghost" size="sm" onClick={() => setDetail(r)}>
                      Detail
                    </Button>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </div>

      <Dialog open={detail !== null} onOpenChange={(o) => !o && setDetail(null)}>
        <DialogContent className="max-h-[85vh] overflow-auto">
          {detail && (
            <>
              <DialogHeader>
                <DialogTitle>{detail.childName}</DialogTitle>
              </DialogHeader>
              <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
                <Field label="Datum narození" value={new Date(detail.birthDate).toLocaleDateString("cs-CZ")} />
                <Field label="Rodné číslo" value={detail.personalId} />
                <Field label="Adresa" value={detail.childAddress} />
                <Field label="Pojišťovna" value={detail.healthInsurance} />
                <Field label="Třída" value={detail.className} />
                <Field label="Program" value={detail.programName} />
                <Field label="Rodič" value={detail.parentName} />
                <Field label="Telefon" value={detail.parentPhone} />
                <Field label="E-mail" value={detail.parentEmail} />
                <Field label="Druhý rodič" value={detail.secondParentName} />
                <Field label="Telefon 2" value={detail.secondParentPhone} />
                <Field label="Dres" value={detail.wantsShirt ? detail.shirtSize ?? "ano" : "ne"} />
                <Field label="Přezdívka" value={detail.nickName} />
                <Field label="Alergie" value={detail.allergies} />
                <Field label="Poznámka" value={detail.note} />
                <Field label="Cena" value={`${detail.priceSnapshot} Kč`} />
                <Field label="Souhlas OÚ" value={detail.consentPersonalData ? "ano" : "ne"} />
                <Field label="Souhlas média" value={detail.consentMedia ? "ano" : "ne"} />
              </dl>

              <div className="mt-6 flex flex-wrap items-center gap-2 border-t border-[var(--border)] pt-4">
                <span className="text-sm text-[var(--muted-foreground)]">Platba:</span>
                {(["unpaid", "paid"] as const).map((s) => (
                  <Button
                    key={s}
                    size="sm"
                    variant={detail.paymentStatus === s ? "default" : "outline"}
                    disabled={isPending || detail.status === "cancelled"}
                    onClick={() =>
                      startTransition(async () => {
                        await setPaymentStatus(detail.id, s);
                        router.refresh();
                        setDetail(null);
                      })
                    }
                  >
                    {s === "paid" ? "Zaplaceno" : "Nezaplaceno"}
                  </Button>
                ))}
                {detail.status !== "cancelled" && (
                  <Button
                    size="sm"
                    variant="destructive"
                    className="ml-auto"
                    disabled={isPending}
                    onClick={() => {
                      if (!confirm("Opravdu zrušit tuto registraci? Uvolní se místo v programu.")) return;
                      startTransition(async () => {
                        await cancelRegistration(detail.id);
                        router.refresh();
                        setDetail(null);
                      });
                    }}
                  >
                    Zrušit registraci
                  </Button>
                )}
              </div>
            </>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}

function Field({ label, value }: { label: string; value?: string | null }) {
  return (
    <>
      <dt className="text-[var(--muted-foreground)]">{label}</dt>
      <dd className="font-medium">{value || "—"}</dd>
    </>
  );
}
