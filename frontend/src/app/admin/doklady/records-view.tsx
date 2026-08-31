"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { Plus, Download, Trash2, Receipt, FileText, FileSignature, File } from "lucide-react";
import type { RecordDocument, Trainer } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Select } from "@/components/ui/select";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { EmptyState } from "@/components/page-header";
import { deleteRecord } from "@/lib/actions";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm";

const TYPE_META: Record<RecordDocument["type"], { label: string; icon: typeof Receipt; variant: "info" | "success" | "warning" | "default" }> = {
  receipt: { label: "Účtenka", icon: Receipt, variant: "info" },
  dpp: { label: "DPP", icon: FileText, variant: "success" },
  contract: { label: "Smlouva", icon: FileSignature, variant: "warning" },
  other: { label: "Ostatní", icon: File, variant: "default" },
};

const TYPE_TABS: { value: string; label: string }[] = [
  { value: "", label: "Vše" },
  { value: "receipt", label: "Účtenky" },
  { value: "dpp", label: "DPP" },
  { value: "contract", label: "Smlouvy" },
  { value: "other", label: "Ostatní" },
];

function fmtDate(iso?: string | null) {
  return iso ? new Date(iso).toLocaleDateString("cs-CZ") : "—";
}

function czk(n?: number | null) {
  return n != null ? n.toLocaleString("cs-CZ") + " Kč" : "—";
}

function fmtSize(bytes?: number | null) {
  if (!bytes) return "";
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} kB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

export function RecordsView({
  records,
  trainers,
  activeType,
}: {
  records: RecordDocument[];
  trainers: Trainer[];
  activeType: string;
}) {
  const router = useRouter();
  const toast = useToast();
  const confirm = useConfirm();
  const [pending, start] = useTransition();
  const [open, setOpen] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Form state
  const [type, setType] = useState<string>("receipt");
  const [title, setTitle] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [docDate, setDocDate] = useState("");
  const [amount, setAmount] = useState("");
  const [personId, setPersonId] = useState("");
  const [note, setNote] = useState("");

  function pickType(t: string) {
    router.push(`/admin/doklady${t ? `?type=${t}` : ""}`);
  }

  function openDialog() {
    setType("receipt");
    setTitle("");
    setFile(null);
    setDocDate(new Date().toISOString().slice(0, 10));
    setAmount("");
    setPersonId("");
    setNote("");
    setError(null);
    setOpen(true);
  }

  async function upload() {
    if (!file) {
      setError("Vyber soubor (PDF nebo obrázek).");
      return;
    }
    setUploading(true);
    setError(null);
    const fd = new FormData();
    fd.append("file", file);
    fd.append("type", type);
    if (title) fd.append("title", title);
    if (docDate) fd.append("docDate", docDate);
    if (amount) fd.append("amount", amount.replace(",", "."));
    if (personId) fd.append("personId", personId);
    if (note) fd.append("note", note);

    try {
      const res = await fetch("/api/records", { method: "POST", body: fd });
      const data = await res.json().catch(() => null);
      if (!res.ok) {
        setError(data?.error ?? "Nahrání selhalo");
        return;
      }
      setOpen(false);
      router.refresh();
    } catch {
      setError("Nahrání selhalo");
    } finally {
      setUploading(false);
    }
  }

  async function remove(r: RecordDocument) {
    if (!(await confirm({ message: `Smazat doklad „${r.title}"? Soubor bude odstraněn.`, danger: true, confirmLabel: "Smazat" }))) return;
    start(async () => {
      const res = await deleteRecord(r.id);
      if (res.ok) router.refresh();
      else toast.error(res.error ?? "Smazání selhalo");
    });
  }

  return (
    <div>
      {/* Type tabs + upload */}
      <div className="mb-4 flex flex-wrap items-center gap-2">
        <div className="flex flex-wrap gap-2">
          {TYPE_TABS.map((t) => (
            <button
              key={t.value}
              onClick={() => pickType(t.value)}
              className={`rounded-full border px-3 py-1.5 text-sm font-medium ${
                activeType === t.value
                  ? "border-[var(--primary)] bg-[var(--primary)] text-[var(--primary-foreground)]"
                  : "border-[var(--border)] hover:bg-[var(--muted)]"
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>
        <Button size="sm" className="ml-auto" onClick={openDialog}>
          <Plus className="h-4 w-4" /> Nahrát doklad
        </Button>
      </div>

      {records.length === 0 ? (
        <EmptyState
          icon={Receipt}
          message="Zatím žádné doklady. Nahraj účtenku, DPP nebo smlouvu."
        />
      ) : (
        <div className="rounded-lg border border-[var(--border)] bg-[var(--background)]">
          <div className="overflow-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Typ</TableHead>
                  <TableHead>Název</TableHead>
                  <TableHead>Osoba</TableHead>
                  <TableHead>Datum</TableHead>
                  <TableHead>Částka</TableHead>
                  <TableHead className="text-right">Akce</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {records.map((r) => {
                  const meta = TYPE_META[r.type];
                  const Icon = meta.icon;
                  return (
                    <TableRow key={r.id}>
                      <TableCell>
                        <Badge variant={meta.variant}>
                          <Icon className="mr-1 h-3 w-3" />
                          {meta.label}
                        </Badge>
                      </TableCell>
                      <TableCell className="font-medium">
                        {r.title}
                        {r.sizeBytes ? (
                          <span className="ml-1 text-xs text-[var(--muted-foreground)]">
                            ({fmtSize(r.sizeBytes)})
                          </span>
                        ) : null}
                        {r.note && (
                          <div className="text-xs font-normal text-[var(--muted-foreground)]">{r.note}</div>
                        )}
                      </TableCell>
                      <TableCell className="text-sm">{r.personName ?? "—"}</TableCell>
                      <TableCell className="whitespace-nowrap text-sm text-[var(--muted-foreground)]">
                        {fmtDate(r.docDate)}
                      </TableCell>
                      <TableCell className="whitespace-nowrap">{czk(r.amount)}</TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-1">
                          <Button variant="ghost" size="sm" asChild>
                            <a href={`/api/records/${r.id}/download`} target="_blank" rel="noreferrer">
                              <Download className="h-4 w-4" /> Otevřít
                            </a>
                          </Button>
                          <Button variant="ghost" size="sm" onClick={() => remove(r)} disabled={pending}>
                            <Trash2 className="h-4 w-4 text-[var(--destructive)]" />
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </div>
        </div>
      )}

      {/* Upload dialog */}
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Nahrát doklad</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4">
            <div>
              <Label>Typ</Label>
              <Select value={type} onChange={(e) => setType(e.target.value)}>
                <option value="receipt">Účtenka</option>
                <option value="dpp">DPP (dohoda)</option>
                <option value="contract">Smlouva</option>
                <option value="other">Ostatní</option>
              </Select>
            </div>
            <div>
              <Label>Soubor (PDF nebo obrázek)</Label>
              <input
                type="file"
                accept="application/pdf,image/*"
                onChange={(e) => setFile(e.target.files?.[0] ?? null)}
                className="w-full rounded-md border border-[var(--border)] bg-[var(--background)] px-3 py-2 text-sm file:mr-3 file:rounded file:border-0 file:bg-[var(--muted)] file:px-3 file:py-1 file:text-sm"
              />
            </div>
            <div>
              <Label>Název (nepovinné)</Label>
              <Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="např. Účtenka za dresy" />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <Label>Datum</Label>
                <Input type="date" value={docDate} onChange={(e) => setDocDate(e.target.value)} />
              </div>
              {type === "receipt" && (
                <div>
                  <Label>Částka (Kč)</Label>
                  <Input type="number" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} />
                </div>
              )}
            </div>
            {(type === "dpp" || type === "contract") && (
              <div>
                <Label>Osoba</Label>
                <Select value={personId} onChange={(e) => setPersonId(e.target.value)}>
                  <option value="">— vyber osobu —</option>
                  {trainers.map((t) => (
                    <option key={t.id} value={t.id}>{t.name}</option>
                  ))}
                </Select>
              </div>
            )}
            <div>
              <Label>Poznámka (nepovinné)</Label>
              <Input value={note} onChange={(e) => setNote(e.target.value)} />
            </div>

            {error && <p className="text-sm text-[var(--destructive)]">{error}</p>}

            <div className="flex justify-end gap-2">
              <Button variant="outline" onClick={() => setOpen(false)} disabled={uploading}>Zrušit</Button>
              <Button onClick={upload} disabled={uploading}>{uploading ? "Nahrávám…" : "Nahrát"}</Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
