"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { Download, Check, Ban, Settings, Search, Mail } from "lucide-react";
import type { Invoice, SupplierSettings } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { markInvoicePaid, cancelInvoice, saveSupplierSettings, lookupAres, sendInvoiceEmail } from "@/lib/actions";

const STATUS: Record<string, { label: string; variant: "success" | "warning" | "danger" }> = {
  paid: { label: "Zaplaceno", variant: "success" },
  unpaid: { label: "Nezaplaceno", variant: "warning" },
  cancelled: { label: "Storno", variant: "danger" },
};

function czk(n: number) {
  return n.toLocaleString("cs-CZ") + " Kč";
}

export function BillingView({
  invoices,
  supplier,
}: {
  invoices: Invoice[];
  supplier: SupplierSettings;
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
        <InvoicesTable invoices={invoices} hasIban={!!supplier.iban} />
      ) : (
        <SupplierForm supplier={supplier} />
      )}
    </div>
  );
}

function InvoicesTable({ invoices, hasIban }: { invoices: Invoice[]; hasIban: boolean }) {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();

  const unpaidSum = invoices.filter((i) => i.status === "unpaid").reduce((s, i) => s + i.totalAmount, 0);
  const paidSum = invoices.filter((i) => i.status === "paid").reduce((s, i) => s + i.totalAmount, 0);

  if (invoices.length === 0) {
    return (
      <div className="rounded-lg border border-dashed border-[var(--border)] bg-[var(--background)] p-12 text-center text-sm text-[var(--muted-foreground)]">
        Zatím žádné faktury. Faktury vytvoříš u registrace (Registrace → detail → Vystavit fakturu).
      </div>
    );
  }

  return (
    <div>
      {!hasIban && (
        <div className="panel-warning mb-4 rounded-lg border p-3 text-sm">
          Nastav IBAN v „Nastavení dodavatele", aby faktury obsahovaly QR platbu.
        </div>
      )}
      <div className="mb-4 grid gap-4 sm:grid-cols-2">
        <Card>
          <CardContent className="p-4">
            <p className="text-sm text-[var(--muted-foreground)]">Nezaplaceno</p>
            <p className="mt-1 text-2xl font-bold text-warning">{czk(unpaidSum)}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <p className="text-sm text-[var(--muted-foreground)]">Zaplaceno</p>
            <p className="mt-1 text-2xl font-bold text-success">{czk(paidSum)}</p>
          </CardContent>
        </Card>
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
                  <TableCell>
                    <Badge variant={st.variant}>{st.label}</Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-1">
                      <Button variant="ghost" size="sm" asChild>
                        <a href={`/api/billing/invoices/${inv.id}/pdf`} target="_blank" rel="noreferrer">
                          <Download className="h-4 w-4" /> PDF
                        </a>
                      </Button>
                      {inv.status !== "cancelled" && inv.payerEmail && (
                        <Button
                          variant="ghost"
                          size="sm"
                          disabled={isPending}
                          title={`Odeslat na ${inv.payerEmail}`}
                          onClick={() =>
                            startTransition(async () => {
                              const res = await sendInvoiceEmail(inv.id);
                              alert(res.ok ? "Faktura odeslána e-mailem." : res.error ?? "Odeslání selhalo");
                            })
                          }
                        >
                          <Mail className="h-4 w-4" /> Odeslat
                        </Button>
                      )}
                      {inv.status === "unpaid" && (
                        <>
                          <Button
                            variant="ghost"
                            size="sm"
                            disabled={isPending}
                            onClick={() =>
                              startTransition(async () => {
                                await markInvoicePaid(inv.id);
                                router.refresh();
                              })
                            }
                          >
                            <Check className="h-4 w-4" /> Zaplaceno
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            disabled={isPending}
                            onClick={() => {
                              if (!confirm(`Stornovat fakturu ${inv.invoiceNumber}?`)) return;
                              startTransition(async () => {
                                await cancelInvoice(inv.id);
                                router.refresh();
                              });
                            }}
                          >
                            <Ban className="h-4 w-4" />
                          </Button>
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
        alert(res.error ?? "Uložení selhalo");
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
