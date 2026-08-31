"use client";

import { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Upload, Check, CircleAlert, HelpCircle, Ban, RefreshCw } from "lucide-react";
import type { BankMatch } from "@/lib/types";
import { Button } from "@/components/ui/button";
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

function czk(n?: number | null) {
  return n != null ? n.toLocaleString("cs-CZ") + " Kč" : "—";
}

function status(m: BankMatch): string {
  return (m.matchStatus || "").toLowerCase();
}

const STATUS_META: Record<string, { label: string; variant: "success" | "warning" | "danger" | "info" | "default"; icon: typeof Check }> = {
  exact: { label: "Přesná shoda", variant: "success", icon: Check },
  partial: { label: "Částka nesedí", variant: "warning", icon: CircleAlert },
  none: { label: "Bez faktury", variant: "default", icon: HelpCircle },
  already: { label: "Už importováno", variant: "info", icon: RefreshCw },
  outgoing: { label: "Odchozí", variant: "default", icon: Ban },
};

export function BankImportView() {
  const router = useRouter();
  const fileRef = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [matches, setMatches] = useState<BankMatch[] | null>(null);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<string | null>(null);

  async function loadPreview() {
    if (!file) {
      setError("Vyber soubor s výpisem (CSV).");
      return;
    }
    setLoading(true);
    setError(null);
    setResult(null);
    const fd = new FormData();
    fd.append("file", file);
    try {
      const res = await fetch("/api/billing/bank-import/preview", { method: "POST", body: fd });
      const data = await res.json();
      if (!res.ok) {
        setError(data?.error ?? "Načtení selhalo");
        return;
      }
      const items: BankMatch[] = data.items ?? [];
      setMatches(items);
      // Pre-select exact matches that aren't already paid.
      const pre = new Set<string>();
      for (const m of items) {
        if (status(m) === "exact" && !m.invoiceAlreadyPaid) pre.add(m.txId);
      }
      setSelected(pre);
    } catch {
      setError("Načtení selhalo");
    } finally {
      setLoading(false);
    }
  }

  function toggle(txId: string) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(txId)) next.delete(txId);
      else next.add(txId);
      return next;
    });
  }

  async function applySelected() {
    if (!file || selected.size === 0) return;
    if (!window.confirm(`Označit ${selected.size} plateb jako zaplacené?`)) return;
    setLoading(true);
    setError(null);
    const fd = new FormData();
    fd.append("file", file);
    for (const id of selected) fd.append("txIds", id);
    try {
      const res = await fetch("/api/billing/bank-import/confirm", { method: "POST", body: fd });
      const data = await res.json();
      if (!res.ok) {
        setError(data?.error ?? "Potvrzení selhalo");
        return;
      }
      setResult(`Hotovo — označeno ${data.paid} faktur jako zaplacené (importováno ${data.imported} transakcí).`);
      setMatches(null);
      setFile(null);
      setSelected(new Set());
      if (fileRef.current) fileRef.current.value = "";
      router.refresh();
    } catch {
      setError("Potvrzení selhalo");
    } finally {
      setLoading(false);
    }
  }

  const selectableCount = matches?.filter((m) => {
    const s = status(m);
    return (s === "exact" || s === "partial") && !m.invoiceAlreadyPaid;
  }).length ?? 0;

  return (
    <div className="space-y-4">
      <Card>
        <CardContent className="flex flex-wrap items-center gap-3 p-4">
          <input
            ref={fileRef}
            type="file"
            accept=".csv,text/csv"
            onChange={(e) => {
              setFile(e.target.files?.[0] ?? null);
              setMatches(null);
              setResult(null);
            }}
            className="text-sm file:mr-3 file:rounded file:border-0 file:bg-[var(--muted)] file:px-3 file:py-1.5 file:text-sm"
          />
          <Button onClick={loadPreview} disabled={loading || !file}>
            <Upload className="h-4 w-4" /> {loading ? "Načítám…" : "Načíst výpis"}
          </Button>
        </CardContent>
      </Card>

      {error && (
        <div className="panel-danger rounded-lg border p-3 text-sm">{error}</div>
      )}
      {result && (
        <div className="rounded-lg border p-3 text-sm" style={{ background: "var(--success-bg)", color: "var(--success-fg)", borderColor: "var(--success-border)" }}>
          {result}
        </div>
      )}

      {matches && (
        <>
          <div className="flex items-center justify-between">
            <p className="text-sm text-[var(--muted-foreground)]">
              {matches.length} transakcí · {selectableCount} spárovatelných · {selected.size} vybráno
            </p>
            <Button onClick={applySelected} disabled={loading || selected.size === 0}>
              <Check className="h-4 w-4" /> Označit vybrané jako zaplacené
            </Button>
          </div>

          <div className="rounded-lg border border-[var(--border)] bg-[var(--background)]">
            <div className="overflow-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-10"></TableHead>
                    <TableHead>Datum</TableHead>
                    <TableHead>Plátce / zpráva</TableHead>
                    <TableHead>VS</TableHead>
                    <TableHead>Částka</TableHead>
                    <TableHead>Faktura</TableHead>
                    <TableHead>Stav</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {matches.map((m) => {
                    const s = status(m);
                    const meta = STATUS_META[s] ?? STATUS_META.none;
                    const canSelect = (s === "exact" || s === "partial") && !m.invoiceAlreadyPaid;
                    return (
                      <TableRow key={m.txId}>
                        <TableCell>
                          {canSelect && (
                            <input
                              type="checkbox"
                              checked={selected.has(m.txId)}
                              onChange={() => toggle(m.txId)}
                              className="h-4 w-4"
                            />
                          )}
                        </TableCell>
                        <TableCell className="whitespace-nowrap text-sm text-[var(--muted-foreground)]">
                          {m.txDate ? new Date(m.txDate).toLocaleDateString("cs-CZ") : "—"}
                        </TableCell>
                        <TableCell className="text-sm">
                          {m.counterparty ?? "—"}
                          {m.message && (
                            <div className="text-xs text-[var(--muted-foreground)]">{m.message}</div>
                          )}
                        </TableCell>
                        <TableCell className="tabular-nums">{m.variableSymbol ?? "—"}</TableCell>
                        <TableCell className="whitespace-nowrap font-medium">{czk(m.amount)}</TableCell>
                        <TableCell className="text-sm">
                          {m.invoiceNumber ? (
                            <>
                              {m.invoiceNumber}
                              {m.invoiceAmount != null && m.invoiceAmount !== m.amount && (
                                <div className="text-xs text-warning">faktura {czk(m.invoiceAmount)}</div>
                              )}
                              {m.invoiceAlreadyPaid && (
                                <div className="text-xs text-success">už zaplaceno</div>
                              )}
                            </>
                          ) : (
                            "—"
                          )}
                        </TableCell>
                        <TableCell>
                          <Badge variant={meta.variant}>{meta.label}</Badge>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
