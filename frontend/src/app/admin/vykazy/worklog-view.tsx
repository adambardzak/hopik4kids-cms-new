"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { Plus, Check, X, Trash2, Download, CalendarClock, Pencil } from "lucide-react";
import type { Program, WorkLog, WorkLogSummary } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Select } from "@/components/ui/select";
import { Card, CardContent } from "@/components/ui/card";
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
import {
  createWorkLog,
  updateWorkLog,
  deleteWorkLog,
  setWorkLogStatus,
  seedWorkLogsFromShifts,
} from "@/lib/actions";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm";

const STATUS: Record<WorkLog["status"], { label: string; variant: "success" | "warning" | "danger" }> = {
  approved: { label: "Schváleno", variant: "success" },
  pending: { label: "Čeká", variant: "warning" },
  rejected: { label: "Zamítnuto", variant: "danger" },
};

function fmtDate(iso: string) {
  return new Date(iso).toLocaleDateString("cs-CZ");
}

function fmtHours(h: number) {
  return `${h.toLocaleString("cs-CZ")} h`;
}

export function WorkLogView({
  logs,
  summary,
  programs,
  privileged,
  from,
  to,
}: {
  logs: WorkLog[];
  summary: WorkLogSummary[];
  programs: Program[];
  privileged: boolean;
  from: string;
  to: string;
}) {
  const router = useRouter();
  const toast = useToast();
  const confirm = useConfirm();
  const [pending, start] = useTransition();

  const [periodFrom, setPeriodFrom] = useState(from);
  const [periodTo, setPeriodTo] = useState(to);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<WorkLog | null>(null);
  const [fDate, setFDate] = useState("");
  const [fHours, setFHours] = useState("");
  const [fNote, setFNote] = useState("");
  const [fProgram, setFProgram] = useState("");

  function applyPeriod() {
    const q = new URLSearchParams();
    if (periodFrom) q.set("from", periodFrom);
    if (periodTo) q.set("to", periodTo);
    router.push(`/admin/vykazy?${q.toString()}`);
  }

  function openCreate() {
    setEditing(null);
    setFDate(new Date().toISOString().slice(0, 10));
    setFHours("");
    setFNote("");
    setFProgram("");
    setDialogOpen(true);
  }

  function openEdit(w: WorkLog) {
    setEditing(w);
    setFDate(w.workDate);
    setFHours(String(w.hours));
    setFNote(w.note ?? "");
    setFProgram(w.programId ?? "");
    setDialogOpen(true);
  }

  function save() {
    const hours = parseFloat(fHours.replace(",", "."));
    if (!fDate || isNaN(hours) || hours <= 0) {
      toast.error("Vyplň datum a kladný počet hodin.");
      return;
    }
    const body = { workDate: fDate, hours, note: fNote || undefined, programId: fProgram || undefined };
    start(async () => {
      const res = editing ? await updateWorkLog(editing.id, body) : await createWorkLog(body);
      if (res.ok) {
        setDialogOpen(false);
        router.refresh();
      } else {
        toast.error(res.error ?? "Uložení selhalo");
      }
    });
  }

  async function remove(w: WorkLog) {
    if (!(await confirm({ message: `Smazat výkaz ${fmtDate(w.workDate)} (${fmtHours(w.hours)})?`, danger: true, confirmLabel: "Smazat" }))) return;
    start(async () => {
      const res = await deleteWorkLog(w.id);
      if (res.ok) router.refresh();
      else toast.error(res.error ?? "Smazání selhalo");
    });
  }

  function changeStatus(w: WorkLog, status: string) {
    start(async () => {
      const res = await setWorkLogStatus(w.id, status);
      if (res.ok) router.refresh();
      else toast.error(res.error ?? "Změna se nezdařila");
    });
  }

  function seedFromShifts() {
    start(async () => {
      const res = await seedWorkLogsFromShifts(periodFrom, periodTo);
      if (res.ok) {
        toast.success(
          res.created && res.created > 0
            ? `Načteno ${res.created} výkazů ze schválených směn.`
            : "Žádné nové směny k načtení (nebo už jsou načtené).",
        );
        router.refresh();
      } else {
        toast.error(res.error ?? "Import selhal");
      }
    });
  }

  const totalHours = logs.reduce((s, w) => s + w.hours, 0);
  const exportQuery = new URLSearchParams({ from: periodFrom, to: periodTo, format: "xlsx" }).toString();

  return (
    <div className="space-y-5">
      {/* Toolbar: period + actions */}
      <div className="flex flex-wrap items-end gap-3 rounded-lg border border-[var(--border)] bg-[var(--background)] p-3">
        <div>
          <Label className="text-xs">Od</Label>
          <Input type="date" value={periodFrom} onChange={(e) => setPeriodFrom(e.target.value)} className="h-9" />
        </div>
        <div>
          <Label className="text-xs">Do</Label>
          <Input type="date" value={periodTo} onChange={(e) => setPeriodTo(e.target.value)} className="h-9" />
        </div>
        <Button size="sm" onClick={applyPeriod} disabled={pending}>Zobrazit období</Button>
        <Button variant="outline" size="sm" onClick={seedFromShifts} disabled={pending}>
          <CalendarClock className="h-4 w-4" /> Načíst ze směn
        </Button>
        <div className="ml-auto flex gap-2">
          {privileged && (
            <Button variant="outline" size="sm" asChild>
              <a href={`/api/work-logs/export?${exportQuery}`}>
                <Download className="h-4 w-4" /> Export
              </a>
            </Button>
          )}
          <Button size="sm" onClick={openCreate}>
            <Plus className="h-4 w-4" /> Přidat hodiny
          </Button>
        </div>
      </div>

      {/* Admin summary per trainer */}
      {privileged && summary.length > 0 && (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {summary.map((s) => (
            <Card key={s.trainerId}>
              <CardContent className="p-4">
                <p className="font-medium">{s.trainerName}</p>
                <p className="mt-1 text-2xl font-bold text-success">{fmtHours(s.approvedHours)}</p>
                <p className="text-xs text-[var(--muted-foreground)]">
                  schváleno
                  {s.pendingHours > 0 && ` · ${fmtHours(s.pendingHours)} čeká`}
                </p>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Entries table */}
      {logs.length === 0 ? (
        <EmptyState
          icon={CalendarClock}
          message="V tomto období nejsou žádné výkazy. Přidej hodiny nebo je načti ze schválených směn."
        />
      ) : (
        <div className="rounded-lg border border-[var(--border)] bg-[var(--background)]">
          <div className="flex items-center justify-between border-b border-[var(--border)] p-3 text-sm">
            <span className="font-medium">Celkem v období: {fmtHours(totalHours)}</span>
          </div>
          <div className="overflow-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Datum</TableHead>
                  {privileged && <TableHead>Osoba</TableHead>}
                  <TableHead>Hodiny</TableHead>
                  <TableHead>Program / poznámka</TableHead>
                  <TableHead>Stav</TableHead>
                  <TableHead className="text-right">Akce</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {logs.map((w) => {
                  const st = STATUS[w.status];
                  return (
                    <TableRow key={w.id}>
                      <TableCell className="whitespace-nowrap font-medium">{fmtDate(w.workDate)}</TableCell>
                      {privileged && <TableCell>{w.trainerName}</TableCell>}
                      <TableCell className="whitespace-nowrap tabular-nums">{fmtHours(w.hours)}</TableCell>
                      <TableCell className="text-sm">
                        {w.programName ?? w.note ?? "—"}
                        {w.source === "shift" && (
                          <span className="ml-1 text-xs text-[var(--muted-foreground)]">(ze směny)</span>
                        )}
                      </TableCell>
                      <TableCell>
                        <Badge variant={st.variant}>{st.label}</Badge>
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-1">
                          {privileged && w.status !== "approved" && (
                            <Button variant="ghost" size="sm" title="Schválit" onClick={() => changeStatus(w, "approved")} disabled={pending}>
                              <Check className="h-4 w-4 text-success" />
                            </Button>
                          )}
                          {privileged && w.status !== "rejected" && (
                            <Button variant="ghost" size="sm" title="Zamítnout" onClick={() => changeStatus(w, "rejected")} disabled={pending}>
                              <X className="h-4 w-4 text-danger" />
                            </Button>
                          )}
                          {(w.status !== "approved" || privileged) && (
                            <Button variant="ghost" size="sm" title="Upravit" onClick={() => openEdit(w)} disabled={pending}>
                              <Pencil className="h-4 w-4" />
                            </Button>
                          )}
                          {(w.status !== "approved" || privileged) && (
                            <Button variant="ghost" size="sm" title="Smazat" onClick={() => remove(w)} disabled={pending}>
                              <Trash2 className="h-4 w-4 text-[var(--destructive)]" />
                            </Button>
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
      )}

      {/* Add / edit dialog */}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editing ? "Upravit výkaz" : "Přidat hodiny"}</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4">
            <div>
              <Label>Datum</Label>
              <Input type="date" value={fDate} onChange={(e) => setFDate(e.target.value)} />
            </div>
            <div>
              <Label>Počet hodin</Label>
              <Input
                type="number"
                step="0.25"
                min="0"
                value={fHours}
                onChange={(e) => setFHours(e.target.value)}
                placeholder="např. 1,5"
              />
            </div>
            <div>
              <Label>Program (nepovinné)</Label>
              <Select value={fProgram} onChange={(e) => setFProgram(e.target.value)}>
                <option value="">— žádný —</option>
                {programs.map((p) => (
                  <option key={p.id} value={p.id}>{p.name}</option>
                ))}
              </Select>
            </div>
            <div>
              <Label>Poznámka (nepovinné)</Label>
              <Input value={fNote} onChange={(e) => setFNote(e.target.value)} placeholder="co jsi dělal/a" />
            </div>
            <div className="flex justify-end gap-2">
              <Button variant="outline" onClick={() => setDialogOpen(false)} disabled={pending}>Zrušit</Button>
              <Button onClick={save} disabled={pending}>{pending ? "Ukládám…" : "Uložit"}</Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
