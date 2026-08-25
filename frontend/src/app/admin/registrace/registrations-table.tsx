"use client";

import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState, useTransition } from "react";
import { Search, ChevronDown, ChevronRight, Users, Eye, FileSpreadsheet, FileText, Printer } from "lucide-react";
import type { Program, Registration } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Select } from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { IconAction } from "@/components/ui/icon-action";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { cancelRegistration, setPaymentStatus } from "@/lib/actions";
import { createInvoiceReturningId } from "@/lib/actions";
import { BulkEmailDialog } from "@/components/bulk-email-dialog";
import { WaitlistDialog } from "@/components/waitlist-dialog";

const PAGE_SIZE = 25;

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
  filters: { program: string; paymentStatus: string; q: string };
  exportQuery: string;
}) {
  const router = useRouter();
  const [detail, setDetail] = useState<Registration | null>(null);
  const [isPending, startTransition] = useTransition();
  const [search, setSearch] = useState(filters.q);

  // Debounced fulltext search -> URL.
  useEffect(() => {
    const handle = setTimeout(() => {
      if (search === filters.q) return;
      pushFilters({ q: search });
    }, 350);
    return () => clearTimeout(handle);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search]);

  function pushFilters(next: Partial<typeof filters>) {
    const merged = { ...filters, ...next };
    const q = new URLSearchParams();
    if (merged.program) q.set("program", merged.program);
    if (merged.paymentStatus) q.set("paymentStatus", merged.paymentStatus);
    if (merged.q) q.set("q", merged.q);
    router.push(`/admin/registrace${q.toString() ? `?${q}` : ""}`);
  }

  // Active (non-cancelled) rows drive the summary numbers.
  const active = useMemo(
    () => registrations.filter((r) => r.status !== "cancelled"),
    [registrations],
  );
  const unpaid = active.filter((r) => r.paymentStatus === "unpaid");
  const unpaidSum = unpaid.reduce((s, r) => s + r.priceSnapshot, 0);
  const paidSum = active
    .filter((r) => r.paymentStatus === "paid")
    .reduce((s, r) => s + r.priceSnapshot, 0);

  // Group by program unless a single program is already filtered.
  const grouped = useMemo(() => {
    const map = new Map<string, { name: string; rows: Registration[] }>();
    for (const r of registrations) {
      if (!map.has(r.programId)) map.set(r.programId, { name: r.programName, rows: [] });
      map.get(r.programId)!.rows.push(r);
    }
    return Array.from(map.entries()).map(([id, v]) => ({ id, ...v }));
  }, [registrations]);

  const showGroups = !filters.program && grouped.length > 1;

  return (
    <div>
      {/* Summary cards */}
      <div className="mb-6 grid gap-4 sm:grid-cols-3">
        <SummaryCard label="Aktivních registrací" value={String(active.length)} />
        <SummaryCard
          label="Nezaplaceno"
          value={`${unpaid.length}× · ${unpaidSum.toLocaleString("cs-CZ")} Kč`}
          tone="warning"
        />
        <SummaryCard
          label="Zaplaceno"
          value={`${paidSum.toLocaleString("cs-CZ")} Kč`}
          tone="success"
        />
      </div>

      {/* Toolbar */}
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <div className="relative">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--muted-foreground)]" />
          <Input
            className="w-64 pl-9"
            placeholder="Hledat dítě, rodiče, e-mail…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <Select
          className=""
          value={filters.program}
          onChange={(e) => pushFilters({ program: e.target.value })}
        >
          <option value="">Všechny programy</option>
          {programs.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
        </Select>
        <Select
          className=""
          value={filters.paymentStatus}
          onChange={(e) => pushFilters({ paymentStatus: e.target.value })}
        >
          <option value="">Všechny platby</option>
          <option value="unpaid">Nezaplaceno</option>
          <option value="paid">Zaplaceno</option>
          <option value="cancelled">Storno</option>
        </Select>
        <div className="ml-auto flex gap-2">
          <Button
            size="sm"
            asChild
            className="bg-green-600 text-white hover:bg-green-700"
          >
            <a href={`/api/registrations/export?format=xlsx&${exportQuery}`}>
              <FileSpreadsheet className="h-4 w-4" /> Excel
            </a>
          </Button>
          <Button variant="outline" size="sm" asChild>
            <a href={`/api/registrations/export?format=csv&${exportQuery}`}>
              <FileText className="h-4 w-4" /> CSV
            </a>
          </Button>
          {filters.program && (
            <Button variant="outline" size="sm" asChild>
              <a href={`/api/programs/${filters.program}/attendance`} target="_blank" rel="noreferrer">
                <Printer className="h-4 w-4" /> Docházka PDF
              </a>
            </Button>
          )}
        </div>
      </div>

      {registrations.length === 0 ? (
        <div className="rounded-lg border border-dashed border-[var(--border)] bg-[var(--background)] p-12 text-center text-sm text-[var(--muted-foreground)]">
          Nic nenalezeno pro zadané filtry.
        </div>
      ) : showGroups ? (
        <div className="space-y-3">
          {grouped.map((g) => (
            <ProgramGroup key={g.id} programId={g.id} name={g.name} rows={g.rows} onDetail={setDetail} />
          ))}
        </div>
      ) : (
        <Paginated rows={registrations} onDetail={setDetail} />
      )}

      <DetailDialog
        detail={detail}
        onClose={() => setDetail(null)}
        isPending={isPending}
        onPayment={(id, s) =>
          startTransition(async () => {
            await setPaymentStatus(id, s);
            router.refresh();
            setDetail(null);
          })
        }
        onCancel={(id) =>
          startTransition(async () => {
            await cancelRegistration(id);
            router.refresh();
            setDetail(null);
          })
        }
      />
    </div>
  );
}

function SummaryCard({
  label,
  value,
  tone,
}: {
  label: string;
  value: string;
  tone?: "warning" | "success";
}) {
  const color =
    tone === "warning" ? "text-amber-700" : tone === "success" ? "text-green-700" : "";
  return (
    <Card className="p-4">
      <p className="text-sm text-[var(--muted-foreground)]">{label}</p>
      <p className={`mt-1 text-2xl font-bold ${color}`}>{value}</p>
    </Card>
  );
}

function ProgramGroup({
  programId,
  name,
  rows,
  onDetail,
}: {
  programId: string;
  name: string;
  rows: Registration[];
  onDetail: (r: Registration) => void;
}) {
  const [open, setOpen] = useState(true);
  const active = rows.filter((r) => r.status !== "cancelled").length;
  const unpaid = rows.filter((r) => r.status !== "cancelled" && r.paymentStatus === "unpaid").length;

  return (
    <div className="overflow-hidden rounded-lg border border-[var(--border)] bg-[var(--background)]">
      <div className="flex items-center gap-3 px-4 py-3 hover:bg-[var(--muted)]">
        <button onClick={() => setOpen((o) => !o)} className="flex flex-1 items-center gap-3 text-left">
          {open ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
          <span className="font-semibold">{name}</span>
          <span className="flex items-center gap-1 text-sm text-[var(--muted-foreground)]">
            <Users className="h-3.5 w-3.5" /> {active}
          </span>
          {unpaid > 0 && (
            <Badge variant="warning" className="ml-1">
              {unpaid}× nezaplaceno
            </Badge>
          )}
        </button>
        <a
          href={`/api/programs/${programId}/attendance`}
          target="_blank"
          rel="noreferrer"
          className="flex items-center gap-1.5 rounded-md border border-[var(--border)] px-2.5 py-1.5 text-xs font-medium hover:bg-[var(--background)]"
          title="Docházkový list (PDF)"
        >
          <Printer className="h-3.5 w-3.5" /> Docházka
        </a>
        <BulkEmailDialog programId={programId} programName={name} />
        <WaitlistDialog programId={programId} programName={name} />
      </div>
      {open && <RegRows rows={rows} onDetail={onDetail} />}
    </div>
  );
}

function Paginated({
  rows,
  onDetail,
}: {
  rows: Registration[];
  onDetail: (r: Registration) => void;
}) {
  const [page, setPage] = useState(1);
  const pages = Math.max(1, Math.ceil(rows.length / PAGE_SIZE));
  const slice = rows.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  return (
    <div className="overflow-hidden rounded-lg border border-[var(--border)] bg-[var(--background)]">
      <RegRows rows={slice} onDetail={onDetail} />
      {pages > 1 && (
        <div className="flex items-center justify-between border-t border-[var(--border)] px-4 py-3 text-sm">
          <span className="text-[var(--muted-foreground)]">
            {(page - 1) * PAGE_SIZE + 1}–{Math.min(page * PAGE_SIZE, rows.length)} z {rows.length}
          </span>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" disabled={page === 1} onClick={() => setPage((p) => p - 1)}>
              Předchozí
            </Button>
            <Button variant="outline" size="sm" disabled={page === pages} onClick={() => setPage((p) => p + 1)}>
              Další
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}

function RegRows({
  rows,
  onDetail,
}: {
  rows: Registration[];
  onDetail: (r: Registration) => void;
}) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Dítě</TableHead>
          <TableHead>Rodič</TableHead>
          <TableHead>Dres</TableHead>
          <TableHead>Platba</TableHead>
          <TableHead>Přihlášeno</TableHead>
          <TableHead></TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {rows.map((r) => {
          const pay = PAYMENT_LABELS[r.paymentStatus] ?? PAYMENT_LABELS.unpaid;
          return (
            <TableRow key={r.id} className={r.status === "cancelled" ? "opacity-50" : ""}>
              <TableCell className="font-medium">{r.childName}</TableCell>
              <TableCell>
                <div>{r.parentName}</div>
                <div className="text-xs text-[var(--muted-foreground)]">{r.parentPhone}</div>
              </TableCell>
              <TableCell>{r.wantsShirt ? r.shirtSize ?? "ano" : "—"}</TableCell>
              <TableCell>
                <Badge variant={pay.variant}>{pay.label}</Badge>
              </TableCell>
              <TableCell className="text-sm text-[var(--muted-foreground)]">
                {new Date(r.createdAt).toLocaleDateString("cs-CZ")}
              </TableCell>
              <TableCell className="text-right">
                <IconAction label="Detail" icon={Eye} onClick={() => onDetail(r)} />
              </TableCell>
            </TableRow>
          );
        })}
      </TableBody>
    </Table>
  );
}

function DetailDialog({
  detail,
  onClose,
  isPending,
  onPayment,
  onCancel,
}: {
  detail: Registration | null;
  onClose: () => void;
  isPending: boolean;
  onPayment: (id: string, status: string) => void;
  onCancel: (id: string) => void;
}) {
  return (
    <Dialog open={detail !== null} onOpenChange={(o) => !o && onClose()}>
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
              <InvoiceButton registrationId={detail.id} disabled={detail.status === "cancelled"} />
            </div>

            <div className="mt-3 flex flex-wrap items-center gap-2">
              <span className="text-sm text-[var(--muted-foreground)]">Platba:</span>
              {(["unpaid", "paid"] as const).map((s) => (
                <Button
                  key={s}
                  size="sm"
                  variant={detail.paymentStatus === s ? "default" : "outline"}
                  disabled={isPending || detail.status === "cancelled"}
                  onClick={() => onPayment(detail.id, s)}
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
                    onCancel(detail.id);
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

function InvoiceButton({ registrationId, disabled }: { registrationId: string; disabled?: boolean }) {
  const [pending, start] = useTransition();
  return (
    <Button
      size="sm"
      variant="outline"
      disabled={pending || disabled}
      onClick={() =>
        start(async () => {
          const res = await createInvoiceReturningId(registrationId);
          if (res.ok && res.id) {
            window.open(`/api/billing/invoices/${res.id}/pdf`, "_blank");
          } else {
            alert(res.error ?? "Nepodařilo se vystavit fakturu");
          }
        })
      }
    >
      {pending ? "Vystavuji…" : "Vystavit fakturu (PDF)"}
    </Button>
  );
}
