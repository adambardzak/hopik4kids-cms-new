"use client";

import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState, useTransition } from "react";
import { Search, ChevronDown, ChevronRight, Users, Eye, FileSpreadsheet, FileText, Printer, Camera, ShieldCheck } from "lucide-react";
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

const WEEKDAYS = ["", "Po", "Út", "St", "Čt", "Pá", "So", "Ne"];

/** Human-readable program discriminator: location · weekday time · school part — to tell same-named programs apart. */
function programMeta(r: Registration): string {
  const parts: string[] = [];
  if (r.programLocationName) parts.push(r.programLocationName);
  const day = r.programWeekday ? WEEKDAYS[r.programWeekday] : "";
  const dayTime = [day, r.programTime].filter(Boolean).join(" ");
  if (dayTime) parts.push(dayTime);
  if (r.programSchoolPart) parts.push(r.programSchoolPart === "morning" ? "dopolední" : "odpolední");
  return parts.join(" · ");
}

const PAYMENT_LABELS: Record<string, { label: string; variant: "success" | "warning" | "danger" | "info" }> = {
  paid: { label: "Zaplaceno", variant: "success" },
  unpaid: { label: "Nezaplaceno", variant: "warning" },
  invoice_sent: { label: "Faktura odeslána", variant: "info" },
  overdue: { label: "Po splatnosti", variant: "danger" },
  cancelled: { label: "Storno", variant: "danger" },
};

/** Effective payment badge key: overdue takes precedence over the stored status. */
function effectivePaymentKey(r: { paymentStatus: string; overdue?: boolean }): string {
  if (r.overdue && r.paymentStatus !== "paid" && r.paymentStatus !== "cancelled") {
    return "overdue";
  }
  return r.paymentStatus;
}

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
    // "overdue" is a client-side (derived) filter — keep it in the URL but not sent to the API.
    if (merged.paymentStatus) q.set("paymentStatus", merged.paymentStatus);
    if (merged.q) q.set("q", merged.q);
    router.push(`/admin/registrace${q.toString() ? `?${q}` : ""}`);
  }

  // Apply the client-only "overdue" filter (derived from invoice due dates).
  const visible = useMemo(
    () =>
      filters.paymentStatus === "overdue"
        ? registrations.filter((r) => r.overdue && r.paymentStatus !== "paid" && r.paymentStatus !== "cancelled")
        : registrations,
    [registrations, filters.paymentStatus],
  );

  // Active (non-cancelled) rows drive the summary numbers.
  const active = useMemo(
    () => visible.filter((r) => r.status !== "cancelled"),
    [visible],
  );
  const unpaid = active.filter((r) => r.paymentStatus === "unpaid" || r.paymentStatus === "invoice_sent");
  const unpaidSum = unpaid.reduce((s, r) => s + r.priceSnapshot, 0);
  const paidSum = active
    .filter((r) => r.paymentStatus === "paid")
    .reduce((s, r) => s + r.priceSnapshot, 0);

  // Group by program unless a single program is already filtered.
  const grouped = useMemo(() => {
    const map = new Map<string, { name: string; meta: string; rows: Registration[] }>();
    for (const r of visible) {
      if (!map.has(r.programId)) map.set(r.programId, { name: r.programName, meta: programMeta(r), rows: [] });
      map.get(r.programId)!.rows.push(r);
    }
    return Array.from(map.entries()).map(([id, v]) => ({ id, ...v }));
  }, [visible]);

  const showGroups = !filters.program && grouped.length > 1;

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      {/* Sticky header: compact summary + toolbar (always visible while the list scrolls) */}
      <div className="shrink-0 space-y-3 pb-3">
        {/* Compact summary strip */}
        <div className="flex flex-wrap items-center gap-x-6 gap-y-1 rounded-lg border border-[var(--border)] bg-[var(--background)] px-4 py-2.5 text-sm">
          <span>
            <span className="font-semibold">{active.length}</span>{" "}
            <span className="text-[var(--muted-foreground)]">aktivních</span>
          </span>
          <span className="text-warning">
            <span className="font-semibold">{unpaid.length}×</span> nezaplaceno{" "}
            <span className="text-[var(--muted-foreground)]">
              ({unpaidSum.toLocaleString("cs-CZ")} Kč)
            </span>
          </span>
          <span className="text-success">
            <span className="font-semibold">{paidSum.toLocaleString("cs-CZ")} Kč</span> zaplaceno
          </span>
        </div>

        {/* Toolbar */}
        <div className="flex flex-wrap items-center gap-3">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--muted-foreground)]" />
            <Input
              className="w-full pl-9 sm:w-64"
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
            <option value="invoice_sent">Faktura odeslána</option>
            <option value="overdue">Po splatnosti</option>
            <option value="paid">Zaplaceno</option>
            <option value="cancelled">Storno</option>
          </Select>
          <div className="ml-auto flex gap-2">
            <Button size="sm" asChild className="btn-success">
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
      </div>

      {/* Scrollable list */}
      <div className="min-h-0 flex-1 overflow-auto">
        {registrations.length === 0 ? (
          <div className="rounded-lg border border-dashed border-[var(--border)] bg-[var(--background)] p-12 text-center text-sm text-[var(--muted-foreground)]">
            Nic nenalezeno pro zadané filtry.
          </div>
        ) : showGroups ? (
          <div className="space-y-3">
            {grouped.map((g) => (
              <ProgramGroup key={g.id} programId={g.id} name={g.name} meta={g.meta} rows={g.rows} onDetail={setDetail} />
            ))}
          </div>
        ) : (
          <Paginated rows={registrations} onDetail={setDetail} />
        )}
      </div>

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

function ProgramGroup({
  programId,
  name,
  meta,
  rows,
  onDetail,
}: {
  programId: string;
  name: string;
  meta: string;
  rows: Registration[];
  onDetail: (r: Registration) => void;
}) {
  const [open, setOpen] = useState(true);
  const active = rows.filter((r) => r.status !== "cancelled").length;
  const unpaid = rows.filter((r) => r.status !== "cancelled" && r.paymentStatus === "unpaid").length;

  return (
    <div className="overflow-hidden rounded-lg border border-[var(--border)] bg-[var(--background)]">
      <div className="flex flex-wrap items-center gap-3 px-4 py-3 hover:bg-[var(--muted)]">
        <button onClick={() => setOpen((o) => !o)} className="flex w-full flex-1 items-center gap-3 text-left sm:w-auto">
          {open ? <ChevronDown className="h-4 w-4 shrink-0" /> : <ChevronRight className="h-4 w-4 shrink-0" />}
          <span className="flex min-w-0 flex-col">
            <span className="truncate font-semibold leading-tight">{name}</span>
            {meta && <span className="truncate text-xs text-[var(--muted-foreground)]">{meta}</span>}
          </span>
          <span className="flex shrink-0 items-center gap-1 text-sm text-[var(--muted-foreground)]">
            <Users className="h-3.5 w-3.5" /> {active}
          </span>
          {unpaid > 0 && (
            <Badge variant="warning" className="ml-1 shrink-0">
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
          <TableHead>GDPR</TableHead>
          <TableHead>Přihlášeno</TableHead>
          <TableHead></TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {rows.map((r) => {
          const pay = PAYMENT_LABELS[effectivePaymentKey(r)] ?? PAYMENT_LABELS.unpaid;
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
              <TableCell>
                <div className="flex items-center gap-1.5">
                  <ConsentIcon
                    ok={r.consentMedia}
                    icon={Camera}
                    yes="Souhlas s fotografováním"
                    no="BEZ souhlasu s fotografováním"
                  />
                  <ConsentIcon
                    ok={r.consentPersonalData}
                    icon={ShieldCheck}
                    yes="Souhlas se zpracováním osobních údajů"
                    no="BEZ souhlasu s osobními údaji"
                  />
                </div>
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

/** Small GDPR consent indicator: green icon when granted, muted+struck when not. */
function ConsentIcon({
  ok,
  icon: Icon,
  yes,
  no,
}: {
  ok: boolean;
  icon: React.ComponentType<{ className?: string }>;
  yes: string;
  no: string;
}) {
  return (
    <span title={ok ? yes : no} className="inline-flex">
      <Icon className={`h-4 w-4 ${ok ? "text-success" : "text-[var(--muted-foreground)] opacity-40"}`} />
    </span>
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
            <dl className="grid grid-cols-1 gap-x-4 gap-y-2 text-sm sm:grid-cols-2">
              <Field label="Datum narození" value={new Date(detail.birthDate).toLocaleDateString("cs-CZ")} />
              <Field label="Rodné číslo" value={detail.personalId} />
              <Field label="Adresa" value={detail.childAddress} />
              <Field label="Pojišťovna" value={detail.healthInsurance} />
              <Field label="Třída" value={detail.className} />
              <Field label="Program" value={[detail.programName, programMeta(detail)].filter(Boolean).join(" — ")} />
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
              {(["unpaid", "invoice_sent", "paid"] as const).map((s) => (
                <Button
                  key={s}
                  size="sm"
                  variant={detail.paymentStatus === s ? "default" : "outline"}
                  disabled={isPending || detail.status === "cancelled"}
                  onClick={() => onPayment(detail.id, s)}
                >
                  {s === "paid" ? "Zaplaceno" : s === "invoice_sent" ? "Faktura odeslána" : "Nezaplaceno"}
                </Button>
              ))}
              {detail.overdue && detail.paymentStatus !== "paid" && (
                <Badge variant="danger">Po splatnosti</Badge>
              )}
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
      <dd className="font-medium break-words">{value || "—"}</dd>
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
