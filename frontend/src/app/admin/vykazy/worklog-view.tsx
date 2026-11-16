"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import {
  Plus,
  Check,
  X,
  Trash2,
  Download,
  CalendarClock,
  Pencil,
  ChevronLeft,
  ChevronRight,
  Send,
  Undo2,
  FileText,
  Archive,
} from "lucide-react";
import type { Program, WorkLog, WorkReport, WorkReportStatus } from "@/lib/types";
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
  backfillWorkLogsFromShifts,
  submitWorkReport,
  approveWorkReport,
  returnWorkReport,
} from "@/lib/actions";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm";
import { czk } from "@/lib/format";

const STATUS: Record<WorkLog["status"], { label: string; variant: "success" | "warning" | "danger" }> = {
  approved: { label: "Schváleno", variant: "success" },
  pending: { label: "Upraveno", variant: "warning" },
  rejected: { label: "Zamítnuto", variant: "danger" },
};

const REPORT_STATUS: Record<WorkReportStatus, { label: string; variant: "default" | "info" | "success" | "warning" }> = {
  draft: { label: "Rozpracováno", variant: "default" },
  submitted: { label: "Čeká na schválení", variant: "info" },
  approved: { label: "Schváleno", variant: "success" },
  returned: { label: "Vráceno k opravě", variant: "warning" },
};

const MONTHS = ["leden", "únor", "březen", "duben", "květen", "červen", "červenec", "srpen", "září", "říjen", "listopad", "prosinec"];

function monthLabel(month: string) {
  const [y, m] = month.split("-").map(Number);
  return `${MONTHS[m - 1]} ${y}`;
}

function shiftMonth(month: string, delta: number) {
  const [y, m] = month.split("-").map(Number);
  const d = new Date(Date.UTC(y, m - 1 + delta, 1));
  return `${d.getUTCFullYear()}-${String(d.getUTCMonth() + 1).padStart(2, "0")}`;
}

/** Today in Prague as YYYY-MM-DD (not UTC — avoids yesterday's date after midnight). */
function todayPrague(): string {
  return new Intl.DateTimeFormat("sv-SE", { timeZone: "Europe/Prague" }).format(new Date());
}

/** Mirrors the backend rule: the current month can be submitted from 2 days before its end. */
function submitOpensOn(month: string): string | null {
  const today = todayPrague();
  if (month < today.slice(0, 7)) return null;
  const [y, m] = month.split("-").map(Number);
  const lastDay = new Date(Date.UTC(y, m, 0)).getUTCDate();
  const opens = `${month}-${String(lastDay - 2).padStart(2, "0")}`;
  return today >= opens ? null : opens;
}

function fmtDate(iso: string) {
  return new Date(iso).toLocaleDateString("cs-CZ");
}

function fmtHours(h: number) {
  return `${h.toLocaleString("cs-CZ")} h`;
}

/** Can entries of a person with this report status still be changed by the current user? */
function editable(status: WorkReportStatus | undefined, privileged: boolean) {
  if (status === "approved") return false;
  if (status === "submitted") return privileged;
  return true;
}

export function WorkLogView({
  month,
  logs,
  programs,
  myReport,
  reports,
  privileged,
  accountant,
  currentUserId,
}: {
  month: string;
  logs: WorkLog[];
  programs: Program[];
  myReport: WorkReport | null;
  reports: WorkReport[];
  privileged: boolean;
  accountant: boolean;
  currentUserId: string;
}) {
  const router = useRouter();
  const toast = useToast();
  const confirm = useConfirm();
  const [pending, start] = useTransition();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<WorkLog | null>(null);
  const [fDate, setFDate] = useState("");
  const [fHours, setFHours] = useState("");
  const [fNote, setFNote] = useState("");
  const [fProgram, setFProgram] = useState("");

  const [returnFor, setReturnFor] = useState<WorkReport | null>(null);
  const [returnNote, setReturnNote] = useState("");

  const reportByUser = new Map(reports.map((r) => [r.userId, r]));
  const statusOf = (userId: string): WorkReportStatus | undefined =>
    userId === currentUserId && myReport ? myReport.status : reportByUser.get(userId)?.status;
  const myLocked = !editable(myReport?.status, privileged);

  function goMonth(m: string) {
    router.push(`/admin/vykazy?month=${m}`);
  }

  function openCreate() {
    setEditing(null);
    const today = todayPrague();
    setFDate(today.startsWith(month) ? today : `${month}-01`);
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

  function run(fn: () => Promise<{ ok: boolean; error?: string }>, success?: string, after?: () => void) {
    start(async () => {
      const res = await fn();
      if (res.ok) {
        if (success) toast.success(success);
        after?.();
        router.refresh();
      } else {
        toast.error(res.error ?? "Akce selhala");
      }
    });
  }

  function save() {
    const hours = parseFloat(fHours.replace(",", "."));
    if (!fDate || isNaN(hours) || hours <= 0) {
      toast.error("Vyplň datum a kladný počet hodin.");
      return;
    }
    const body = { workDate: fDate, hours, note: fNote || undefined, programId: fProgram || undefined };
    run(
      () => (editing ? updateWorkLog(editing.id, body) : createWorkLog(body)),
      undefined,
      () => setDialogOpen(false),
    );
  }

  async function remove(w: WorkLog) {
    if (!(await confirm({ message: `Smazat záznam ${fmtDate(w.workDate)} (${fmtHours(w.hours)})?`, danger: true, confirmLabel: "Smazat" }))) return;
    run(() => deleteWorkLog(w.id));
  }

  async function submitMine() {
    if (!myReport) return;
    const amount = myReport.totalAmount != null ? ` (${czk(myReport.totalAmount)})` : "";
    if (!(await confirm({
      message: `Odeslat výkaz za ${monthLabel(month)} ke schválení? ${fmtHours(myReport.totalHours)}${amount}. Po odeslání už nepůjde upravovat.`,
      confirmLabel: "Odeslat výkaz",
    }))) return;
    run(() => submitWorkReport(month), "Výkaz odeslán ke schválení.");
  }

  async function approve(r: WorkReport) {
    if (!(await confirm({
      message: `Schválit výkaz ${r.userName} za ${monthLabel(month)}: ${fmtHours(r.totalHours)}${r.totalAmount != null ? `, ${czk(r.totalAmount)}` : ""}?`,
      confirmLabel: "Schválit",
    }))) return;
    run(() => approveWorkReport(r.userId, month), `Výkaz ${r.userName} schválen.`);
  }

  function sendBack() {
    if (!returnFor) return;
    const r = returnFor;
    run(() => returnWorkReport(r.userId, month, returnNote), `Výkaz ${r.userName} vrácen k opravě.`, () => {
      setReturnFor(null);
      setReturnNote("");
    });
  }

  function backfillFromShifts() {
    start(async () => {
      const res = await backfillWorkLogsFromShifts();
      if (res.ok) {
        toast.success(
          res.created && res.created > 0
            ? `Doplněno ${res.created} záznamů z potvrzených směn.`
            : "Žádné nové směny k načtení (nebo už jsou načtené).",
        );
        router.refresh();
      } else {
        toast.error(res.error ?? "Import selhal");
      }
    });
  }

  const opensOn = submitOpensOn(month);
  const showMine = myReport && (!privileged || myReport.entryCount > 0 || myReport.status !== "draft");
  const approvedReports = reports.filter((r) => r.status === "approved");
  const approvedTotal = approvedReports.reduce((s, r) => s + (r.totalAmount ?? 0), 0);
  const exportQuery = new URLSearchParams({
    from: `${month}-01`,
    to: `${month}-${String(new Date(Number(month.slice(0, 4)), Number(month.slice(5, 7)), 0).getDate()).padStart(2, "0")}`,
    format: "xlsx",
  }).toString();

  return (
    <div className="space-y-6">
      {/* Month switcher + actions */}
      <div className="flex flex-wrap items-center gap-2">
        <div className="inline-flex items-center rounded-lg border border-[var(--border)] bg-[var(--background)]">
          <Button variant="ghost" size="sm" onClick={() => goMonth(shiftMonth(month, -1))} disabled={pending} aria-label="Předchozí měsíc">
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <span className="min-w-[9rem] px-2 text-center font-semibold capitalize">{monthLabel(month)}</span>
          <Button variant="ghost" size="sm" onClick={() => goMonth(shiftMonth(month, 1))} disabled={pending} aria-label="Další měsíc">
            <ChevronRight className="h-4 w-4" />
          </Button>
        </div>
        {privileged && (
          <Button variant="outline" size="sm" onClick={backfillFromShifts} disabled={pending}
            title="Doplní záznamy ke všem už potvrzeným směnám, které ještě záznam nemají">
            <CalendarClock className="h-4 w-4" /> Doplnit ze směn
          </Button>
        )}
        <div className="ml-auto flex gap-2">
          {privileged && (
            <Button variant="outline" size="sm" asChild>
              <a href={`/api/work-logs/export?${exportQuery}`}>
                <Download className="h-4 w-4" /> Export XLSX
              </a>
            </Button>
          )}
          {!accountant && (
            <Button size="sm" onClick={openCreate} disabled={myLocked}
              title={myLocked ? "Výkaz za tento měsíc je odeslaný nebo schválený" : undefined}>
              <Plus className="h-4 w-4" /> Přidat hodiny
            </Button>
          )}
        </div>
      </div>

      {/* My monthly report */}
      {showMine && myReport && (
        <Card>
          <CardContent className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:justify-between">
            <div className="space-y-1">
              <div className="flex flex-wrap items-center gap-2">
                <p className="font-semibold">Můj výkaz za {monthLabel(month)}</p>
                <Badge variant={REPORT_STATUS[myReport.status].variant}>{REPORT_STATUS[myReport.status].label}</Badge>
              </div>
              <p className="text-3xl font-bold tabular-nums">
                {fmtHours(myReport.totalHours)}
                {myReport.totalAmount != null && (
                  <span className="ml-3 text-success">{czk(myReport.totalAmount)}</span>
                )}
              </p>
              <p className="text-sm text-[var(--muted-foreground)]">
                {myReport.entryCount} záznamů
                {myReport.hourlyRate != null
                  ? ` · sazba ${czk(myReport.hourlyRate)}/h`
                  : " · sazbu ti zatím nenastavil jednatel"}
              </p>
              {myReport.status === "returned" && myReport.returnNote && (
                <p className="panel-warning rounded-md border px-3 py-2 text-sm">
                  <strong>Vráceno k opravě:</strong> {myReport.returnNote}
                </p>
              )}
              {(myReport.status === "draft" || myReport.status === "returned") && (
                <p className="text-sm text-[var(--muted-foreground)]">
                  {opensOn
                    ? `Směny se průběžně doplňují. Výkaz půjde odeslat od ${fmtDate(opensOn)}.`
                    : "Zkontroluj směny níže, případně je oprav (tužka), a výkaz odešli."}
                </p>
              )}
            </div>
            <div className="flex flex-wrap gap-2">
              <Button variant="outline" asChild>
                <a href={`/api/work-reports/${myReport.userId}/${month}/pdf`} target="_blank" rel="noreferrer">
                  <FileText className="h-4 w-4" /> PDF
                </a>
              </Button>
              {(myReport.status === "draft" || myReport.status === "returned") && (
                <Button onClick={submitMine} disabled={pending || myReport.entryCount === 0 || opensOn !== null} size="lg">
                  <Send className="h-4 w-4" /> Odeslat ke schválení
                </Button>
              )}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Team monthly reports (owner/admin approve, accountant downloads) */}
      {(privileged || accountant) && (
        <div className="rounded-lg border border-[var(--border)] bg-[var(--background)]">
          <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[var(--border)] p-3">
            <div>
              <p className="font-semibold">Měsíční výkazy týmu</p>
              <p className="text-xs text-[var(--muted-foreground)]">
                Schváleno {approvedReports.length} z {reports.length}
                {approvedReports.length > 0 && ` · k výplatě ${czk(approvedTotal)}`}
              </p>
            </div>
            <Button size="sm" asChild={approvedReports.length > 0} disabled={approvedReports.length === 0}>
              {approvedReports.length > 0 ? (
                <a href={`/api/work-reports/approved-zip?period=${month}`}>
                  <Archive className="h-4 w-4" /> Stáhnout schválené (ZIP)
                </a>
              ) : (
                <span className="inline-flex items-center gap-2"><Archive className="h-4 w-4" /> Stáhnout schválené (ZIP)</span>
              )}
            </Button>
          </div>
          {reports.length === 0 ? (
            <p className="p-6 text-center text-sm text-[var(--muted-foreground)]">
              Za {monthLabel(month)} zatím nikdo nemá odpracované hodiny.
            </p>
          ) : (
            <div className="overflow-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Osoba</TableHead>
                    <TableHead className="text-right">Hodiny</TableHead>
                    <TableHead className="text-right">Sazba</TableHead>
                    <TableHead className="text-right">Odměna</TableHead>
                    <TableHead>Stav</TableHead>
                    <TableHead className="text-right">Akce</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {reports.map((r) => {
                    const st = REPORT_STATUS[r.status];
                    const canPdf = !accountant || r.status === "approved";
                    return (
                      <TableRow key={r.userId}>
                        <TableCell className="font-medium">{r.userName}</TableCell>
                        <TableCell className="text-right tabular-nums">{fmtHours(r.totalHours)}</TableCell>
                        <TableCell className="text-right tabular-nums">
                          {r.hourlyRate != null ? czk(r.hourlyRate) : <span className="text-warning" title="Nastav sazbu v Nastavení → Tým">nenastaveno</span>}
                        </TableCell>
                        <TableCell className="text-right font-medium tabular-nums">{czk(r.totalAmount)}</TableCell>
                        <TableCell>
                          <Badge variant={st.variant}>{st.label}</Badge>
                        </TableCell>
                        <TableCell className="text-right">
                          <div className="flex justify-end gap-1">
                            {privileged && r.status === "submitted" && (
                              <Button size="sm" onClick={() => approve(r)} disabled={pending}>
                                <Check className="h-4 w-4" /> Schválit
                              </Button>
                            )}
                            {privileged && (r.status === "submitted" || r.status === "approved") && (
                              <Button variant="outline" size="sm" onClick={() => setReturnFor(r)} disabled={pending}
                                title="Vrátit k opravě">
                                <Undo2 className="h-4 w-4" /> Vrátit
                              </Button>
                            )}
                            {canPdf && (
                              <Button variant="ghost" size="sm" asChild title="Stáhnout PDF">
                                <a href={`/api/work-reports/${r.userId}/${month}/pdf`} target="_blank" rel="noreferrer">
                                  <FileText className="h-4 w-4" />
                                </a>
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
          )}
        </div>
      )}

      {/* Entries */}
      {!accountant && (logs.length === 0 ? (
        <EmptyState
          icon={CalendarClock}
          message={`Za ${monthLabel(month)} nejsou žádné odpracované hodiny. Hodiny se zapíšou samy po potvrzení směny, nebo je přidej ručně.`}
        />
      ) : (
        <div className="rounded-lg border border-[var(--border)] bg-[var(--background)]">
          <div className="flex items-center justify-between border-b border-[var(--border)] p-3 text-sm">
            <span className="font-medium">
              {privileged ? "Všechny záznamy" : "Moje směny a hodiny"} · {fmtHours(logs.reduce((s, w) => s + w.hours, 0))}
            </span>
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
                  const canEdit = editable(statusOf(w.trainerId), privileged);
                  return (
                    <TableRow key={w.id}>
                      <TableCell className="whitespace-nowrap font-medium">{fmtDate(w.workDate)}</TableCell>
                      {privileged && <TableCell>{w.trainerName}</TableCell>}
                      <TableCell className="whitespace-nowrap tabular-nums">{fmtHours(w.hours)}</TableCell>
                      <TableCell className="text-sm">
                        {w.programName ?? w.note ?? "—"}
                        {w.programName && w.note && w.note !== w.programName && (
                          <span className="ml-1 text-[var(--muted-foreground)]">— {w.note}</span>
                        )}
                        {w.source === "shift" && (
                          <span className="ml-1 text-xs text-[var(--muted-foreground)]">(ze směny)</span>
                        )}
                      </TableCell>
                      <TableCell>
                        <Badge variant={st.variant}>{st.label}</Badge>
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-1">
                          {privileged && canEdit && w.status !== "approved" && (
                            <Button variant="ghost" size="sm" title="Schválit záznam" onClick={() => run(() => setWorkLogStatus(w.id, "approved"))} disabled={pending}>
                              <Check className="h-4 w-4 text-success" />
                            </Button>
                          )}
                          {privileged && canEdit && w.status !== "rejected" && (
                            <Button variant="ghost" size="sm" title="Zamítnout záznam (nezapočítá se)" onClick={() => run(() => setWorkLogStatus(w.id, "rejected"))} disabled={pending}>
                              <X className="h-4 w-4 text-danger" />
                            </Button>
                          )}
                          {canEdit && (
                            <Button variant="ghost" size="sm" title="Opravit" onClick={() => openEdit(w)} disabled={pending}>
                              <Pencil className="h-4 w-4" />
                            </Button>
                          )}
                          {canEdit && (
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
      ))}

      {/* Add / edit dialog */}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editing ? "Opravit záznam" : "Přidat hodiny"}</DialogTitle>
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
              <Input value={fNote} onChange={(e) => setFNote(e.target.value)} placeholder="co jsi dělal/a, proč oprava" />
            </div>
            <div className="flex justify-end gap-2">
              <Button variant="outline" onClick={() => setDialogOpen(false)} disabled={pending}>Zrušit</Button>
              <Button onClick={save} disabled={pending}>{pending ? "Ukládám…" : "Uložit"}</Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      {/* Return report dialog */}
      <Dialog open={returnFor !== null} onOpenChange={(o) => !o && setReturnFor(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Vrátit výkaz k opravě</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4">
            <p className="text-sm text-[var(--muted-foreground)]">
              {returnFor?.userName} uvidí poznámku a bude moct výkaz za {monthLabel(month)} opravit a znovu odeslat.
            </p>
            <div>
              <Label>Co je potřeba opravit</Label>
              <Input value={returnNote} onChange={(e) => setReturnNote(e.target.value)} placeholder="např. 12. 11. chybí odpolední kroužek" />
            </div>
            <div className="flex justify-end gap-2">
              <Button variant="outline" onClick={() => setReturnFor(null)} disabled={pending}>Zrušit</Button>
              <Button onClick={sendBack} disabled={pending}>
                <Undo2 className="h-4 w-4" /> Vrátit k opravě
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
