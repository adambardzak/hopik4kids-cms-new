"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { Download, Check, Ban, Settings, Search, Mail, FileText } from "lucide-react";
import type { Invoice, SupplierSettings } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { IconAction } from "@/components/ui/icon-action";
import { EmptyState } from "@/components/page-header";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { markInvoicePaid, cancelInvoice, saveSupplierSettings, lookupAres, sendInvoiceEmail } from "@/lib/actions";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm";
import { czk } from "@/lib/format";

const STATUS: Record<string, { label: string; variant: "success" | "warning" | "danger" }> = {
  paid: { label: "Zaplaceno", variant: "success" },
  unpaid: { label: "Nezaplaceno", variant: "warning" },
  cancelled: { label: "Storno", variant: "danger" },
};

export function BillingView({
  invoices,
  supplier,
  filters,
}: {
  invoices: Invoice[];
  supplier: SupplierSettings;
  filters: { from?: string; to?: string; status?: string; type?: string };
}) {
  const [tab, setTab] = useState<"invoices" | "supplier">("invoices");

  return (
    <div>
      <div className="mb-4 flex gap-2 border-b border-[var(--border)]">
        <button
          onClick={() => setTab("invoices")}
          className={`-mb-px border-b-2 px-4 py-2 text-sm font-medium ${
            tab === "invoices" ? "border-[var(--primary)] text-[var(--primary)]" : "border-transparent text-[var(--muted-foreground)]"
          }`}
        >
          Faktury
        </button>
        <button
          onClick={() => setTab("supplier")}
          className={`-mb-px flex items-center gap-1.5 border-b-2 px-4 py-2 text-sm font-medium ${
            tab === "supplier" ? "border-[var(--primary)] text-[var(--primary)]" : "border-transparent text-[var(--muted-foreground)]"
          }`}
        >
          <Settings className="h-3.5 w-3.5" /> Nastavení dodavatele
        </button>
      </div>

      {tab === "invoices" ? (
        <InvoicesTable invoices={invoices} hasIban={!!supplier.iban} filters={filters} />
      ) : (
        <SupplierForm supplier={supplier} />
      )}
    </div>
  );
}

function InvoicesTable({
  invoices,
  hasIban,
  filters,
}: {
  invoices: Invoice[];
  hasIban: boolean;
  filters: { from?: string; to?: string; status?: string; type?: string };
}) {
  const router = useRouter();
  const toast = useToast();
  const confirm = useConfirm();
  const [isPending, startTransition] = useTransition();

  const [from, setFrom] = useState(filters.from ?? "");
  const [to, setTo] = useState(filters.to ?? "");
  const [status, setStatus] = useState(filters.status ?? "");
  const [type, setType] = useState(filters.type ?? "");

  function applyFilters() {
    const q = new URLSearchParams();
    if (from) q.set("from", from);
    if (to) q.set("to", to);
    if (status) q.set("status", status);
    if (type) q.set("type", type);
    const qs = q.toString();
    router.push(`/admin/fakturace${qs ? `?${qs}` : ""}`);
  }

  function resetFilters() {
    setFrom("");
    setTo("");
    setStatus("");
    setType("");
    router.push("/admin/fakturace");
  }

  const exportQuery = (() => {
    const q = new URLSearchParams();
    if (from) q.set("from", from);
    if (to) q.set("to", to);
    if (status) q.set("status", status);
    if (type) q.set("type", type);
    return q.toString();
  })();

  const unpaidSum = invoices.filter((i) => i.status === "unpaid").reduce((s, i) => s + i.totalAmount, 0);
  const paidSum = invoices.filter((i) => i.status === "paid").reduce((s, i) => s + i.totalAmount, 0);

  if (invoices.length === 0) {
    return (
      <EmptyState
        icon={FileText}
        message="Zatím žádné faktury. Faktury vytvoříš u registrace (Registrace → detail → Vystavit fakturu)."
      />
    );
  }

  return (
    <div>
      {!hasIban && (
        <div className="panel-warning mb-4 rounded-lg border p-3 text-sm">
          Nastav IBAN v „Nastavení dodavatele", aby faktury obsahovaly QR platbu.
        </div>
      )}
      {/* Inline summary */}
      <div className="mb-4 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm">
        <span className="text-warning">
          <span className="font-semibold">{czk(unpaidSum)}</span> nezaplaceno
        </span>
        <span className="text-[var(--border)]">·</span>
        <span className="text-success">
          <span className="font-semibold">{czk(paidSum)}</span> zaplaceno
        </span>
      </div>

      {/* Filters + export on a single row */}
      <div className="mb-4 flex flex-wrap items-end gap-2">
        <div>
          <Label className="text-xs">Od</Label>
          <Input type="date" value={from} onChange={(e) => setFrom(e.target.value)} className="h-9" />
        </div>
        <div>
          <Label className="text-xs">Do</Label>
          <Input type="date" value={to} onChange={(e) => setTo(e.target.value)} className="h-9" />
        </div>
        <div className="w-auto min-w-[140px] max-w-[170px]">
          <Label className="text-xs">Stav</Label>
          <Select value={status} onChange={(e) => setStatus(e.target.value)} className="h-9">
            <option value="">Vše</option>
            <option value="unpaid">Nezaplaceno</option>
            <option value="paid">Zaplaceno</option>
            <option value="cancelled">Storno</option>
          </Select>
        </div>
        <div className="w-auto min-w-[130px] max-w-[160px]">
          <Label className="text-xs">Typ</Label>
          <Select value={type} onChange={(e) => setType(e.target.value)} className="h-9">
            <option value="">Vše</option>
            <option value="club">Kroužek</option>
            <option value="school">Škola</option>
            <option value="camp">Kemp</option>
          </Select>
        </div>
        <Button size="sm" onClick={applyFilters} disabled={isPending}>
          <Search className="h-4 w-4" /> Filtrovat
        </Button>
        <Button variant="ghost" size="sm" onClick={resetFilters} disabled={isPending}>
          Zrušit
        </Button>
        <div className="ml-auto flex gap-2">
          <Button variant="outline" size="sm" asChild>
            <a href={`/api/billing/invoices/export?format=xlsx${exportQuery ? `&${exportQuery}` : ""}`}>
              <Download className="h-4 w-4" /> Excel
            </a>
          </Button>
          <Button variant="outline" size="sm" asChild>
            <a href={`/api/billing/invoices/export?format=csv${exportQuery ? `&${exportQuery}` : ""}`}>
              <Download className="h-4 w-4" /> CSV
            </a>
          </Button>
        </div>
      </div>

      <div className="rounded-lg border border-[var(--border)] bg-[var(--background)]">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Číslo</TableHead>
              <TableHead>Plátce</TableHead>
              <TableHead>Vystaveno</TableHead>
              <TableHead>Splatnost</TableHead>
              <TableHead>Částka</TableHead>
              <TableHead>Kroužek</TableHead>
              <TableHead>Dres</TableHead>
              <TableHead>Stav</TableHead>
              <TableHead></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {invoices.map((inv) => {
              const st = STATUS[inv.status] ?? STATUS.unpaid;
              return (
                <TableRow key={inv.id}>
                  <TableCell className="font-medium">{inv.invoiceNumber}</TableCell>
                  <TableCell>{inv.payerName}</TableCell>
                  <TableCell className="text-sm text-[var(--muted-foreground)]">
                    {new Date(inv.issueDate).toLocaleDateString("cs-CZ")}
                  </TableCell>
                  <TableCell className="text-sm text-[var(--muted-foreground)]">
                    {new Date(inv.dueDate).toLocaleDateString("cs-CZ")}
                  </TableCell>
                  <TableCell>{czk(inv.totalAmount)}</TableCell>
                  <TableCell className="text-sm text-[var(--muted-foreground)]">{czk(inv.programAmount)}</TableCell>
                  <TableCell className="text-sm text-[var(--muted-foreground)]">
                    {inv.shirtAmount > 0 ? czk(inv.shirtAmount) : "—"}
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
                              if (!(await confirm({ message: `Stornovat fakturu ${inv.invoiceNumber}?`, danger: true, confirmLabel: "Stornovat" }))) return;
                              startTransition(async () => {
                                await cancelInvoice(inv.id);
                                router.refresh();
                              });
                            }}
                          />
                        </>
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

function SupplierForm({ supplier }: { supplier: SupplierSettings }) {
  const router = useRouter();
  const toast = useToast();
  const [form, setForm] = useState<SupplierSettings>(supplier);
  const [saved, setSaved] = useState(false);
  const [isPending, startTransition] = useTransition();
  const [aresPending, startAres] = useTransition();
  const [aresError, setAresError] = useState<string | null>(null);

  function set<K extends keyof SupplierSettings>(key: K, value: SupplierSettings[K]) {
    setForm((f) => ({ ...f, [key]: value }));
    setSaved(false);
  }

  function loadFromAres() {
    setAresError(null);
    startAres(async () => {
      const res = await lookupAres(form.ico ?? "");
      if (!res.ok || !res.data) {
        setAresError(res.error ?? "Nepodařilo se načíst z ARESu");
        return;
      }
      setForm((f) => ({
        ...f,
        name: res.data!.name ?? f.name,
        address: res.data!.address ?? f.address,
        dic: res.data!.dic ?? f.dic,
      }));
      setSaved(false);
    });
  }

  function submit() {
    startTransition(async () => {
      const res = await saveSupplierSettings({ ...form, defaultDueDays: Number(form.defaultDueDays) || 14 });
      if (res.ok) {
        setSaved(true);
        router.refresh();
      } else {
        toast.error(res.error ?? "Uložení selhalo");
      }
    });
  }

  return (
    <Card className="max-w-2xl">
      <CardContent className="grid gap-4 p-6">
        <Field label="Název">
          <Input value={form.name ?? ""} onChange={(e) => set("name", e.target.value)} />
        </Field>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label="IČO">
            <div className="flex gap-2">
              <Input value={form.ico ?? ""} onChange={(e) => set("ico", e.target.value)} />
              <Button
                type="button"
                variant="outline"
                onClick={loadFromAres}
                disabled={aresPending || !form.ico}
                title="Načíst název, adresu a DIČ z ARESu"
              >
                <Search className="h-4 w-4" />
                {aresPending ? "…" : "ARES"}
              </Button>
            </div>
            {aresError && <p className="mt-1 text-xs text-[var(--destructive)]">{aresError}</p>}
          </Field>
          <Field label="DIČ (jen pokud plátce DPH)">
            <Input value={form.dic ?? ""} onChange={(e) => set("dic", e.target.value)} />
          </Field>
        </div>
        <Field label="Adresa">
          <Input value={form.address ?? ""} onChange={(e) => set("address", e.target.value)} />
        </Field>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label="IBAN (pro QR platbu)">
            <Input
              value={form.iban ?? ""}
              placeholder="CZ65 0800 ..."
              onChange={(e) => set("iban", e.target.value)}
            />
          </Field>
          <Field label="Číslo účtu (na faktuře)">
            <Input value={form.accountNumber ?? ""} onChange={(e) => set("accountNumber", e.target.value)} />
          </Field>
        </div>
        <Field label="Splatnost (dní)">
          <Input
            type="number"
            className="w-32"
            value={form.defaultDueDays ?? 14}
            onChange={(e) => set("defaultDueDays", Number(e.target.value))}
          />
        </Field>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label="Web">
            <Input value={form.web ?? ""} placeholder="www.hopik4kids.cz" onChange={(e) => set("web", e.target.value)} />
          </Field>
          <Field label="E-mail">
            <Input value={form.email ?? ""} placeholder="info@hopik4kids.cz" onChange={(e) => set("email", e.target.value)} />
          </Field>
        </div>
        <Field label="Text v patičce faktury">
          <Input value={form.footerText ?? ""} onChange={(e) => set("footerText", e.target.value)} />
        </Field>

        <div className="flex items-center gap-3">
          <Button onClick={submit} disabled={isPending || !form.name}>
            {isPending ? "Ukládám…" : saved ? "Uloženo ✓" : "Uložit"}
          </Button>
          {!form.dic && (
            <span className="text-xs text-[var(--muted-foreground)]">
              Bez DIČ = na faktuře „Dodavatel není plátcem DPH".
            </span>
          )}
        </div>
      </CardContent>
    </Card>
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
