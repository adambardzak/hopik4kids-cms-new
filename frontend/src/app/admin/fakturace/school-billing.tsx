"use client";

import { useRouter } from "next/navigation";
import { useMemo, useState, useTransition } from "react";
import {
  Ban,
  Check,
  Download,
  FileText,
  Mail,
  Pencil,
  Plus,
  ReceiptText,
  School,
  Search,
  Trash2,
} from "lucide-react";
import type { CreditNote, Invoice, Location, Program, SchoolContract, SchoolContractInput } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { IconAction } from "@/components/ui/icon-action";
import { EmptyState } from "@/components/page-header";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  cancelInvoice,
  createCreditNote,
  createSchoolInvoice,
  deleteSchoolContract,
  lookupAres,
  markInvoicePaid,
  saveSchoolContract,
  sendCreditNoteEmail,
  sendInvoiceEmail,
} from "@/lib/actions";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm";
import { czk } from "@/lib/format";

const WEEKDAYS = ["", "Pondělí", "Úterý", "Středa", "Čtvrtek", "Pátek", "Sobota", "Neděle"];

const PRICE_UNITS: Record<string, { label: string; short: string }> = {
  lesson: { label: "za lekci", short: "/lekce" },
  month: { label: "za měsíc", short: "/měs." },
  period: { label: "za celé období", short: "/období" },
};

const INVOICE_STATUS: Record<string, { label: string; variant: "success" | "warning" | "danger" }> = {
  paid: { label: "Zaplaceno", variant: "success" },
  unpaid: { label: "Nezaplaceno", variant: "warning" },
  cancelled: { label: "Storno", variant: "danger" },
};

function czDate(iso?: string | null) {
  return iso ? new Date(iso).toLocaleDateString("cs-CZ") : "—";
}

/** "Pondělí 9:00–9:45" from the program fields. */
function slotOf(c: SchoolContract) {
  const day = c.weekday ? WEEKDAYS[c.weekday] : "";
  if (!c.time) return day || "—";
  const [h, m] = c.time.split(":").map(Number);
  let end = "";
  if (c.durationMin && Number.isFinite(h) && Number.isFinite(m)) {
    const total = h * 60 + m + c.durationMin;
    end = `–${String(Math.floor(total / 60) % 24).padStart(2, "0")}:${String(total % 60).padStart(2, "0")}`;
  }
  return `${day} ${c.time}${end}`.trim();
}

/**
 * "Faktury školky": kindergartens we run morning programs for. Lesson data is stored on the
 * program (hence it shows up in the calendar, shifts and attendance); this page adds the billing
 * data and issues invoices to the kindergarten.
 */
export function SchoolBilling({
  contracts,
  candidates,
  locations,
  invoices,
  creditNotes,
}: {
  contracts: SchoolContract[];
  candidates: Program[];
  locations: Location[];
  invoices: Invoice[];
  creditNotes: CreditNote[];
}) {
  const router = useRouter();
  const toast = useToast();
  const confirm = useConfirm();
  const [isPending, startTransition] = useTransition();
  const [editing, setEditing] = useState<SchoolContract | "new" | null>(null);
  const [invoicing, setInvoicing] = useState<SchoolContract | null>(null);

  // Invoices (and their totals) per kindergarten.
  const byContract = useMemo(() => {
    const m = new Map<string, Invoice[]>();
    for (const inv of invoices) {
      if (!inv.schoolContractId) continue;
      const list = m.get(inv.schoolContractId) ?? [];
      list.push(inv);
      m.set(inv.schoolContractId, list);
    }
    return m;
  }, [invoices]);

  async function remove(c: SchoolContract) {
    const ok = await confirm({
      message: `Odebrat fakturační údaje školky „${c.schoolName}"? Program zůstane v kalendáři i ve směnách, zmizí jen z tohoto seznamu.`,
      danger: true,
      confirmLabel: "Odebrat",
    });
    if (!ok) return;
    startTransition(async () => {
      const res = await deleteSchoolContract(c.id);
      if (!res.ok) toast.error(res.error ?? "Odebrání selhalo");
      router.refresh();
    });
  }

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center gap-2">
        <p className="text-sm text-[var(--muted-foreground)]">
          Školky, kde vedeme dopolední programy. Den, čas i období se propisují do rozvrhu a kalendáře.
        </p>
        <Button className="ml-auto" onClick={() => setEditing("new")}>
          <Plus className="h-4 w-4" /> Přidat školku
        </Button>
      </div>

      {contracts.length === 0 ? (
        <EmptyState
          icon={School}
          message={'Zatím žádná školka. Přidej ji tlačítkem „Přidat školku" — zadané údaje se propíšou do kalendáře.'}
        />
      ) : (
        <div className="rounded-lg border border-[var(--border)] bg-[var(--background)]">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Školka</TableHead>
                <TableHead>Místo</TableHead>
                <TableHead>Den a čas</TableHead>
                <TableHead>Období</TableHead>
                <TableHead>Trenéři</TableHead>
                <TableHead>Cena</TableHead>
                <TableHead>E-mail</TableHead>
                <TableHead>Fakturováno</TableHead>
                <TableHead></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {contracts.map((c) => {
                const issued = byContract.get(c.id) ?? [];
                const invoiced = issued
                  .filter((i) => i.status !== "cancelled")
                  .reduce((s, i) => s + i.totalAmount, 0);
                const unit = PRICE_UNITS[c.priceUnit] ?? PRICE_UNITS.lesson;
                return (
                  <TableRow key={c.id}>
                    <TableCell className="font-medium">{c.schoolName}</TableCell>
                    <TableCell className="text-sm text-[var(--muted-foreground)]">
                      {c.locationName ?? "—"}
                    </TableCell>
                    <TableCell className="text-sm">{slotOf(c)}</TableCell>
                    <TableCell className="text-sm text-[var(--muted-foreground)]">
                      {c.periodFrom || c.periodTo
                        ? `${czDate(c.periodFrom)} – ${czDate(c.periodTo)}`
                        : "—"}
                      {c.lessonsInPeriod > 0 && (
                        <span className="block text-xs">{c.lessonsInPeriod} lekcí</span>
                      )}
                    </TableCell>
                    <TableCell className="text-sm">{c.trainersNeeded}</TableCell>
                    <TableCell className="text-sm">
                      {czk(c.price)}
                      <span className="text-[var(--muted-foreground)]">{unit.short}</span>
                      {c.periodAmount > 0 && c.priceUnit !== "period" && (
                        <span className="block text-xs text-[var(--muted-foreground)]">
                          celkem {czk(c.periodAmount)}
                        </span>
                      )}
                    </TableCell>
                    <TableCell className="text-sm text-[var(--muted-foreground)]">
                      {c.billingEmail ?? <span className="text-warning">chybí</span>}
                    </TableCell>
                    <TableCell className="text-sm">
                      {issued.length === 0 ? (
                        <span className="text-[var(--muted-foreground)]">—</span>
                      ) : (
                        <>
                          {czk(invoiced)}
                          <span className="block text-xs text-[var(--muted-foreground)]">
                            {issued.length}× faktura
                          </span>
                        </>
                      )}
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-1">
                        <IconAction
                          label="Vystavit fakturu"
                          icon={ReceiptText}
                          disabled={isPending}
                          onClick={() => setInvoicing(c)}
                        />
                        <IconAction label="Upravit" icon={Pencil} onClick={() => setEditing(c)} />
                        <IconAction
                          label="Odebrat ze seznamu"
                          icon={Trash2}
                          className="text-[var(--destructive)]"
                          disabled={isPending}
                          onClick={() => remove(c)}
                        />
                      </div>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </div>
      )}

      <SchoolInvoicesTable invoices={invoices} contracts={contracts} creditNotes={creditNotes} />

      {editing && (
        <ContractDialog
          contract={editing === "new" ? null : editing}
          candidates={candidates}
          locations={locations}
          onClose={() => setEditing(null)}
        />
      )}
      {invoicing && (
        <IssueInvoiceDialog contract={invoicing} onClose={() => setInvoicing(null)} />
      )}
    </div>
  );
}

/** Issued kindergarten invoices with the same actions as the children's invoice table. */
function SchoolInvoicesTable({
  invoices,
  contracts,
  creditNotes,
}: {
  invoices: Invoice[];
  contracts: SchoolContract[];
  creditNotes: CreditNote[];
}) {
  const router = useRouter();
  const toast = useToast();
  const confirm = useConfirm();
  const [isPending, startTransition] = useTransition();

  const schoolName = useMemo(() => {
    const m = new Map(contracts.map((c) => [c.id, c.schoolName]));
    return (id?: string | null) => (id ? m.get(id) ?? "—" : "—");
  }, [contracts]);

  if (invoices.length === 0) return null;

  const unpaidSum = invoices.filter((i) => i.status === "unpaid").reduce((s, i) => s + i.totalAmount, 0);
  const paidSum = invoices.filter((i) => i.status === "paid").reduce((s, i) => s + i.totalAmount, 0);

  return (
    <div className="mt-8">
      <div className="mb-3 flex flex-wrap items-center gap-x-4 gap-y-1">
        <h2 className="text-base font-semibold">Vystavené faktury školkám</h2>
        <span className="text-sm text-warning">
          <span className="font-semibold">{czk(unpaidSum)}</span> nezaplaceno
        </span>
        <span className="text-sm text-success">
          <span className="font-semibold">{czk(paidSum)}</span> zaplaceno
        </span>
        <Button variant="outline" size="sm" className="ml-auto" asChild>
          <a href="/api/billing/invoices/export?format=xlsx&payerKind=school">
            <Download className="h-4 w-4" /> Excel
          </a>
        </Button>
      </div>
      <div className="rounded-lg border border-[var(--border)] bg-[var(--background)]">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Číslo</TableHead>
              <TableHead>Školka</TableHead>
              <TableHead>Období</TableHead>
              <TableHead>Vystaveno</TableHead>
              <TableHead>Splatnost</TableHead>
              <TableHead>Částka</TableHead>
              <TableHead>Posláno</TableHead>
              <TableHead>Stav</TableHead>
              <TableHead></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {invoices.map((inv) => {
              const st = INVOICE_STATUS[inv.status] ?? INVOICE_STATUS.unpaid;
              const creditNote = creditNotes.find((c) => c.invoiceId === inv.id);
              return (
                <TableRow key={inv.id}>
                  <TableCell className="font-medium">{inv.invoiceNumber}</TableCell>
                  <TableCell>{inv.payerName || schoolName(inv.schoolContractId)}</TableCell>
                  <TableCell className="text-sm text-[var(--muted-foreground)]">
                    {inv.periodFrom ? `${czDate(inv.periodFrom)} – ${czDate(inv.periodTo)}` : "—"}
                  </TableCell>
                  <TableCell className="text-sm text-[var(--muted-foreground)]">
                    {czDate(inv.issueDate)}
                  </TableCell>
                  <TableCell className="text-sm text-[var(--muted-foreground)]">
                    {czDate(inv.dueDate)}
                  </TableCell>
                  <TableCell>{czk(inv.totalAmount)}</TableCell>
                  <TableCell>
                    <SentCell invoice={inv} />
                  </TableCell>
                  <TableCell>
                    <Badge variant={st.variant}>{st.label}</Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-1">
                      <IconAction
                        label="Stáhnout PDF"
                        icon={Download}
                        href={`/api/billing/invoices/${inv.id}/pdf`}
                      />
                      {inv.status !== "cancelled" && inv.payerEmail && (
                        <IconAction
                          label={`Odeslat na ${inv.payerEmail}`}
                          icon={Mail}
                          disabled={isPending}
                          onClick={() =>
                            startTransition(async () => {
                              const res = await sendInvoiceEmail(inv.id);
                              if (res.ok) toast.success("Faktura odeslána e-mailem.");
                              else toast.error(res.error ?? "Odeslání selhalo");
                              router.refresh();
                            })
                          }
                        />
                      )}
                      {inv.status === "unpaid" && (
                        <>
                          <IconAction
                            label="Označit jako zaplaceno"
                            icon={Check}
                            className="text-[var(--success-fg)]"
                            disabled={isPending}
                            onClick={() =>
                              startTransition(async () => {
                                await markInvoicePaid(inv.id);
                                router.refresh();
                              })
                            }
                          />
                          <IconAction
                            label="Stornovat fakturu"
                            icon={Ban}
                            className="text-[var(--destructive)]"
                            disabled={isPending}
                            onClick={async () => {
                              if (
                                !(await confirm({
                                  message: `Stornovat fakturu ${inv.invoiceNumber}?`,
                                  danger: true,
                                  confirmLabel: "Stornovat",
                                }))
                              )
                                return;
                              startTransition(async () => {
                                await cancelInvoice(inv.id);
                                router.refresh();
                              });
                            }}
                          />
                        </>
                      )}
                      {creditNote ? (
                        <>
                          <IconAction
                            label={`Stáhnout dobropis ${creditNote.number}`}
                            icon={FileText}
                            className="text-[var(--destructive)]"
                            href={`/api/billing/credit-notes/${creditNote.id}/pdf`}
                          />
                          {creditNote.payerEmail && (
                            <IconAction
                              label={`Odeslat dobropis na ${creditNote.payerEmail}`}
                              icon={Mail}
                              className="text-[var(--destructive)]"
                              disabled={isPending}
                              onClick={() =>
                                startTransition(async () => {
                                  const res = await sendCreditNoteEmail(creditNote.id);
                                  if (res.ok) toast.success("Dobropis odeslán e-mailem.");
                                  else toast.error(res.error ?? "Odeslání selhalo");
                                })
                              }
                            />
                          )}
                        </>
                      ) : (
                        inv.status !== "cancelled" && (
                          <IconAction
                            label="Vystavit dobropis"
                            icon={FileText}
                            disabled={isPending}
                            onClick={async () => {
                              if (
                                !(await confirm({
                                  message: `Vystavit dobropis k faktuře ${inv.invoiceNumber}? Faktura bude stornována.`,
                                  confirmLabel: "Vystavit dobropis",
                                }))
                              )
                                return;
                              startTransition(async () => {
                                const res = await createCreditNote(inv.id);
                                if (res.ok) toast.success("Dobropis vystaven.");
                                else toast.error(res.error ?? "Vystavení dobropisu selhalo");
                                router.refresh();
                              });
                            }}
                          />
                        )
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}

/** "Sent" marker — stamped the moment the invoice is e-mailed from the app. */
export function SentCell({ invoice }: { invoice: Invoice }) {
  if (!invoice.sentAt) {
    return <span className="text-sm text-[var(--muted-foreground)]">—</span>;
  }
  return (
    <span className="text-sm text-success" title={new Date(invoice.sentAt).toLocaleString("cs-CZ")}>
      Posláno {czDate(invoice.sentAt)}
      {invoice.sentCount > 1 && (
        <span className="text-[var(--muted-foreground)]"> ({invoice.sentCount}×)</span>
      )}
    </span>
  );
}

type FormState = SchoolContractInput & { id?: string };

function ContractDialog({
  contract,
  candidates,
  locations,
  onClose,
}: {
  contract: SchoolContract | null;
  candidates: Program[];
  locations: Location[];
  onClose: () => void;
}) {
  const router = useRouter();
  const [form, setForm] = useState<FormState>(
    contract
      ? {
          id: contract.id,
          schoolName: contract.schoolName,
          locationId: contract.locationId,
          weekday: contract.weekday,
          time: contract.time,
          durationMin: contract.durationMin,
          periodFrom: contract.periodFrom,
          periodTo: contract.periodTo,
          trainersNeeded: contract.trainersNeeded,
          billingName: contract.billingName,
          billingEmail: contract.billingEmail,
          billingAddress: contract.billingAddress,
          ico: contract.ico,
          dic: contract.dic,
          price: contract.price,
          priceUnit: contract.priceUnit,
          note: contract.note,
        }
      : { priceUnit: "lesson", price: 0, trainersNeeded: 1, durationMin: 45 },
  );
  const [error, setError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();
  const [aresPending, startAres] = useTransition();

  function set<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  /** Linking an existing school program prefills the lesson fields from it. */
  function linkProgram(programId: string) {
    const p = candidates.find((c) => c.id === programId);
    setForm((f) => ({
      ...f,
      programId: programId || null,
      schoolName: p?.name ?? f.schoolName,
      locationId: p?.locationId ?? f.locationId,
      weekday: p?.weekday ?? f.weekday,
      time: p?.time ?? f.time,
      durationMin: p?.durationMin ?? f.durationMin,
      periodFrom: p?.validFrom ?? f.periodFrom,
      periodTo: p?.validTo ?? f.periodTo,
      trainersNeeded: p?.trainersNeeded ?? f.trainersNeeded,
    }));
  }

  function loadFromAres() {
    startAres(async () => {
      const res = await lookupAres(form.ico ?? "");
      if (!res.ok || !res.data) {
        setError(res.error ?? "Nepodařilo se načíst z ARESu");
        return;
      }
      setForm((f) => ({
        ...f,
        billingName: res.data!.name ?? f.billingName,
        billingAddress: res.data!.address ?? f.billingAddress,
        dic: res.data!.dic ?? f.dic,
      }));
      setError(null);
    });
  }

  function submit() {
    setError(null);
    if (!form.programId && !form.schoolName?.trim()) {
      setError("Zadej název školky (nebo napoj existující program).");
      return;
    }
    const body: SchoolContractInput = {
      programId: form.programId || null,
      schoolName: form.schoolName?.trim() || null,
      locationId: form.locationId || null,
      weekday: form.weekday ? Number(form.weekday) : null,
      time: form.time || null,
      durationMin: form.durationMin ? Number(form.durationMin) : null,
      periodFrom: form.periodFrom || null,
      periodTo: form.periodTo || null,
      trainersNeeded: form.trainersNeeded ? Number(form.trainersNeeded) : 1,
      billingName: form.billingName || null,
      billingEmail: form.billingEmail || null,
      billingAddress: form.billingAddress || null,
      ico: form.ico || null,
      dic: form.dic || null,
      price: Number(form.price ?? 0),
      priceUnit: form.priceUnit ?? "lesson",
      note: form.note || null,
    };
    startTransition(async () => {
      const res = await saveSchoolContract(form.id ?? null, body);
      if (!res.ok) {
        setError(res.error ?? "Uložení selhalo");
        return;
      }
      onClose();
      router.refresh();
    });
  }

  return (
    <Dialog open onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-h-[85vh] max-w-2xl overflow-auto">
        <DialogHeader>
          <DialogTitle>{form.id ? "Upravit školku" : "Nová školka"}</DialogTitle>
        </DialogHeader>
        <div className="grid gap-4">
          {!form.id && candidates.length > 0 && (
            <Field label="Napojit už zadaný program (nepovinné)">
              <Select
                className="w-full"
                value={form.programId ?? ""}
                onChange={(e) => linkProgram(e.target.value)}
              >
                <option value="">— vytvořit nový program —</option>
                {candidates.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                    {p.weekday ? ` · ${WEEKDAYS[p.weekday]}` : ""}
                    {p.time ? ` ${p.time}` : ""}
                  </option>
                ))}
              </Select>
              <p className="mt-1 text-xs text-[var(--muted-foreground)]">
                Školky, které už máte v Programech, takhle jen doplníte o fakturaci — nic se nekopíruje.
              </p>
            </Field>
          )}

          <Field label="Název školky">
            <Input value={form.schoolName ?? ""} onChange={(e) => set("schoolName", e.target.value)} />
          </Field>

          <Field label="Místo">
            <Select
              className="w-full"
              value={form.locationId ?? ""}
              onChange={(e) => set("locationId", e.target.value || null)}
            >
              <option value="">— žádné —</option>
              {locations.map((l) => (
                <option key={l.id} value={l.id}>
                  {l.name}
                </option>
              ))}
            </Select>
          </Field>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-4">
            <Field label="Den v týdnu">
              <Select
                className="w-full"
                value={form.weekday ?? ""}
                onChange={(e) => set("weekday", e.target.value ? Number(e.target.value) : null)}
              >
                <option value="">—</option>
                {WEEKDAYS.slice(1).map((d, i) => (
                  <option key={i + 1} value={i + 1}>
                    {d}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="Čas (HH:MM)">
              <Input value={form.time ?? ""} placeholder="09:00" onChange={(e) => set("time", e.target.value)} />
            </Field>
            <Field label="Délka hodiny (min)">
              <Input
                type="number"
                value={form.durationMin ?? ""}
                onChange={(e) => set("durationMin", e.target.value ? Number(e.target.value) : null)}
              />
            </Field>
            <Field label="Počet trenérů">
              <Input
                type="number"
                min={1}
                value={form.trainersNeeded ?? 1}
                onChange={(e) => set("trainersNeeded", e.target.value ? Number(e.target.value) : 1)}
              />
            </Field>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label="Období od (první lekce)">
              <Input
                type="date"
                value={form.periodFrom ?? ""}
                onChange={(e) => set("periodFrom", e.target.value || null)}
              />
            </Field>
            <Field label="Období do (poslední lekce)">
              <Input
                type="date"
                value={form.periodTo ?? ""}
                onChange={(e) => set("periodTo", e.target.value || null)}
              />
            </Field>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label="Cena (Kč)">
              <Input
                type="number"
                min={0}
                value={form.price ?? 0}
                onChange={(e) => set("price", Number(e.target.value))}
              />
            </Field>
            <Field label="Cena je">
              <Select
                className="w-full"
                value={form.priceUnit ?? "lesson"}
                onChange={(e) => set("priceUnit", e.target.value as SchoolContractInput["priceUnit"])}
              >
                <option value="lesson">za lekci</option>
                <option value="month">za měsíc</option>
                <option value="period">za celé období</option>
              </Select>
            </Field>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label="E-mail pro fakturu">
              <Input
                value={form.billingEmail ?? ""}
                placeholder="skolka@example.cz"
                onChange={(e) => set("billingEmail", e.target.value)}
              />
            </Field>
            <Field label="IČO (odběratel na faktuře)">
              <div className="flex gap-2">
                <Input value={form.ico ?? ""} onChange={(e) => set("ico", e.target.value)} />
                <Button
                  type="button"
                  variant="outline"
                  onClick={loadFromAres}
                  disabled={aresPending || !form.ico}
                  title="Načíst název a adresu z ARESu"
                >
                  <Search className="h-4 w-4" />
                  {aresPending ? "…" : "ARES"}
                </Button>
              </div>
            </Field>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label="Odběratel na faktuře (prázdné = název školky)">
              <Input value={form.billingName ?? ""} onChange={(e) => set("billingName", e.target.value)} />
            </Field>
            <Field label="Fakturační adresa">
              <Input
                value={form.billingAddress ?? ""}
                onChange={(e) => set("billingAddress", e.target.value)}
              />
            </Field>
          </div>

          <Field label="Poznámka">
            <Input value={form.note ?? ""} onChange={(e) => set("note", e.target.value)} />
          </Field>

          {error && <p className="text-sm text-[var(--destructive)]">{error}</p>}

          <div className="flex justify-end gap-2">
            <Button variant="ghost" onClick={onClose} disabled={isPending}>
              Zrušit
            </Button>
            <Button onClick={submit} disabled={isPending}>
              {isPending ? "Ukládám…" : "Uložit"}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}

/** Issue an invoice for a period: lesson count and amount are prefilled but editable. */
function IssueInvoiceDialog({
  contract,
  onClose,
}: {
  contract: SchoolContract;
  onClose: () => void;
}) {
  const router = useRouter();
  const toast = useToast();
  const [from, setFrom] = useState(contract.periodFrom ?? "");
  const [to, setTo] = useState(contract.periodTo ?? "");
  const [lessons, setLessons] = useState(String(contract.lessonsInPeriod || ""));
  const [amount, setAmount] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  const unit = PRICE_UNITS[contract.priceUnit] ?? PRICE_UNITS.lesson;

  function submit() {
    setError(null);
    if (!from || !to) {
      setError("Zadej období faktury.");
      return;
    }
    startTransition(async () => {
      const res = await createSchoolInvoice(contract.id, {
        periodFrom: from,
        periodTo: to,
        lessonCount: lessons.trim() === "" ? null : Number(lessons),
        totalAmount: amount.trim() === "" ? null : Number(amount),
      });
      if (!res.ok) {
        setError(res.error ?? "Vystavení faktury selhalo");
        return;
      }
      toast.success("Faktura vystavena. Odeslat ji můžeš ikonou obálky.");
      onClose();
      router.refresh();
    });
  }

  return (
    <Dialog open onOpenChange={(o) => !o && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Vystavit fakturu — {contract.schoolName}</DialogTitle>
        </DialogHeader>
        <div className="grid gap-4">
          <p className="text-sm text-[var(--muted-foreground)]">
            Sjednaná cena {czk(contract.price)} {unit.label}
            {contract.billingEmail ? ` · fakturu pošleme na ${contract.billingEmail}` : " · chybí e-mail školky"}
          </p>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label="Období od">
              <Input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
            </Field>
            <Field label="Období do">
              <Input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
            </Field>
          </div>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label="Počet lekcí (prázdné = podle kalendáře)">
              <Input
                type="number"
                min={0}
                value={lessons}
                onChange={(e) => setLessons(e.target.value)}
              />
            </Field>
            <Field label="Částka celkem (prázdné = spočítat z ceny)">
              <Input
                type="number"
                min={0}
                placeholder={contract.periodAmount ? String(contract.periodAmount) : ""}
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
              />
            </Field>
          </div>
          {error && <p className="text-sm text-[var(--destructive)]">{error}</p>}
          <div className="flex justify-end gap-2">
            <Button variant="ghost" onClick={onClose} disabled={isPending}>
              Zrušit
            </Button>
            <Button onClick={submit} disabled={isPending}>
              {isPending ? "Vystavuji…" : "Vystavit fakturu"}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-1.5">
      <Label>{label}</Label>
      {children}
    </div>
  );
}
