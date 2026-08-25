"use client";

import { useEffect, useState, useTransition } from "react";
import { Check, X, CircleAlert, Save, BarChart3 } from "lucide-react";
import type { AttendanceRow, AttendanceStats, AttendanceStatus, Program } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  saveAttendance,
  fetchAttendanceRoster,
  fetchAttendanceStats,
} from "@/lib/actions";

const WEEKDAYS = ["", "Po", "Út", "St", "Čt", "Pá", "So", "Ne"];

const STATUS_META: Record<AttendanceStatus, { label: string; icon: typeof Check; classes: string; active: string }> = {
  present: {
    label: "Přítomen",
    icon: Check,
    classes: "border-green-200 text-green-700 hover:bg-green-50",
    active: "bg-green-600 text-white border-green-600",
  },
  excused: {
    label: "Omluven",
    icon: CircleAlert,
    classes: "border-amber-200 text-amber-700 hover:bg-amber-50",
    active: "bg-amber-500 text-white border-amber-500",
  },
  absent: {
    label: "Nepřítomen",
    icon: X,
    classes: "border-red-200 text-red-700 hover:bg-red-50",
    active: "bg-red-600 text-white border-red-600",
  },
};

/** Next date (>= today) matching the program's weekday, as ISO. */
function nextLessonDate(weekday?: number | null): string {
  const today = new Date();
  if (!weekday) return today.toISOString().slice(0, 10);
  const d = new Date(today);
  for (let i = 0; i < 7; i++) {
    if (((d.getDay() + 6) % 7) + 1 === weekday) break; // getDay: Sun=0 -> ISO
    d.setDate(d.getDate() + 1);
  }
  return d.toISOString().slice(0, 10);
}

type Draft = Record<string, { status: AttendanceStatus | null; note: string }>;

export function AttendanceView({ programs }: { programs: Program[] }) {
  const [programId, setProgramId] = useState(programs[0]?.id ?? "");
  const program = programs.find((p) => p.id === programId);
  const [date, setDate] = useState(() => nextLessonDate(programs[0]?.weekday));
  const [rows, setRows] = useState<AttendanceRow[]>([]);
  const [draft, setDraft] = useState<Draft>({});
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState(false);
  const [dirty, setDirty] = useState(false);
  const [saved, setSaved] = useState(false);
  const [isPending, startTransition] = useTransition();
  const [stats, setStats] = useState<AttendanceStats | null>(null);
  const [showStats, setShowStats] = useState(false);

  // Load roster on program/date change.
  useEffect(() => {
    if (!programId || !date) return;
    setLoading(true);
    setLoadError(false);
    setSaved(false);
    fetchAttendanceRoster(programId, date).then((res) => {
      if (!res.ok) {
        setLoadError(true);
        setRows([]);
        setDraft({});
        setLoading(false);
        return;
      }
      setRows(res.items);
      const d: Draft = {};
      for (const r of res.items) d[r.childId] = { status: r.status, note: r.note ?? "" };
      setDraft(d);
      setDirty(false);
      setLoading(false);
    });
  }, [programId, date]);

  function confirmDiscard(): boolean {
    if (!dirty) return true;
    return confirm("Máš neuložené změny v docházce. Opravdu je zahodit?");
  }

  function onProgramChange(id: string) {
    if (!confirmDiscard()) return;
    setProgramId(id);
    const p = programs.find((x) => x.id === id);
    setDate(nextLessonDate(p?.weekday));
    setShowStats(false);
  }

  function onDateChange(newDate: string) {
    if (!confirmDiscard()) return;
    setDate(newDate);
  }

  function setStatus(childId: string, status: AttendanceStatus) {
    setDraft((d) => ({
      ...d,
      [childId]: { ...d[childId], status: d[childId]?.status === status ? null : status },
    }));
    setDirty(true);
    setSaved(false);
  }

  function setNote(childId: string, note: string) {
    setDraft((d) => ({ ...d, [childId]: { ...d[childId], note } }));
    setDirty(true);
    setSaved(false);
  }

  function markAllPresent() {
    setDraft((d) => {
      const next = { ...d };
      for (const r of rows) next[r.childId] = { ...next[r.childId], status: "present" };
      return next;
    });
    setDirty(true);
    setSaved(false);
  }

  function save() {
    const entries = rows.map((r) => ({
      childId: r.childId,
      status: draft[r.childId]?.status ?? null,
      note: draft[r.childId]?.note ?? null,
    }));
    startTransition(async () => {
      const res = await saveAttendance(programId, date, entries);
      if (res.ok) {
        setSaved(true);
        setDirty(false);
      } else {
        alert(res.error ?? "Uložení selhalo");
      }
    });
  }

  function openStats() {
    setShowStats(true);
    fetchAttendanceStats(programId).then((res) => setStats(res.stats));
  }

  const recordedCount = Object.values(draft).filter((x) => x.status).length;

  return (
    <div className="space-y-4">
      {/* Controls */}
      <div className="flex flex-wrap items-end gap-3">
        <div className="flex flex-col gap-1.5">
          <Label>Program</Label>
          <Select value={programId} onChange={(e) => onProgramChange(e.target.value)} className="w-64">
            {programs.map((p) => (
              <option key={p.id} value={p.id}>
                {WEEKDAYS[p.weekday ?? 0]} {p.time ?? ""} · {p.name}
              </option>
            ))}
          </Select>
        </div>
        <div className="flex flex-col gap-1.5">
          <Label>Datum lekce</Label>
          <Input type="date" value={date} onChange={(e) => onDateChange(e.target.value)} className="w-44" />
        </div>
        <div className="ml-auto flex gap-2">
          <Button variant="outline" onClick={openStats}>
            <BarChart3 className="h-4 w-4" /> Statistiky
          </Button>
        </div>
      </div>

      {showStats ? (
        <StatsPanel stats={stats} onClose={() => setShowStats(false)} programName={program?.name ?? ""} />
      ) : (
        <>
          {loadError && (
            <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
              Nepodařilo se načíst docházku. Zkontroluj připojení a zkus to prosím znovu.
            </div>
          )}
          <div className="flex items-center justify-between">
            <p className="text-sm text-[var(--muted-foreground)]">
              {loading ? "Načítám…" : `${rows.length} dětí · zaznamenáno ${recordedCount}`}
            </p>
            <div className="flex items-center gap-2">
              <Button variant="outline" size="sm" onClick={markAllPresent} disabled={loading || rows.length === 0}>
                <Check className="h-4 w-4" /> Všichni přítomni
              </Button>
              <Button size="sm" onClick={save} disabled={isPending || loading || rows.length === 0}>
                <Save className="h-4 w-4" /> {saved ? "Uloženo ✓" : isPending ? "Ukládám…" : "Uložit"}
              </Button>
            </div>
          </div>

          <div className="rounded-lg border border-[var(--border)] bg-[var(--background)]">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Dítě</TableHead>
                  <TableHead className="w-[320px]">Stav</TableHead>
                  <TableHead>Poznámka</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {rows.map((r) => {
                  const cur = draft[r.childId]?.status ?? null;
                  return (
                    <TableRow key={r.childId}>
                      <TableCell className="font-medium">{r.childName}</TableCell>
                      <TableCell>
                        <div className="flex gap-1.5">
                          {(Object.keys(STATUS_META) as AttendanceStatus[]).map((s) => {
                            const meta = STATUS_META[s];
                            const Icon = meta.icon;
                            const isActive = cur === s;
                            return (
                              <button
                                key={s}
                                onClick={() => setStatus(r.childId, s)}
                                className={`inline-flex items-center gap-1 rounded-md border px-2.5 py-1.5 text-xs font-medium transition-colors ${
                                  isActive ? meta.active : meta.classes
                                }`}
                              >
                                <Icon className="h-3.5 w-3.5" />
                                {meta.label}
                              </button>
                            );
                          })}
                        </div>
                      </TableCell>
                      <TableCell>
                        <Input
                          className="h-8"
                          placeholder="—"
                          value={draft[r.childId]?.note ?? ""}
                          onChange={(e) => setNote(r.childId, e.target.value)}
                        />
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </div>
        </>
      )}
    </div>
  );
}

function StatsPanel({
  stats,
  onClose,
  programName,
}: {
  stats: AttendanceStats | null;
  onClose: () => void;
  programName: string;
}) {
  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">Statistiky docházky — {programName}</h2>
        <Button variant="outline" size="sm" onClick={onClose}>
          Zpět na zápis
        </Button>
      </div>

      {!stats || (stats.children.length === 0 && stats.lessons.length === 0) ? (
        <Card>
          <CardContent className="p-6 text-sm text-[var(--muted-foreground)]">
            Zatím žádná zaznamenaná docházka pro tento program.
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4 lg:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Podle dítěte</CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Dítě</TableHead>
                    <TableHead className="text-center">Přítomen</TableHead>
                    <TableHead className="text-center">Omluven</TableHead>
                    <TableHead className="text-center">Nepřít.</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {stats.children.map((c) => (
                    <TableRow key={c.childId}>
                      <TableCell className="font-medium">{c.childName}</TableCell>
                      <TableCell className="text-center text-green-700">{c.present}</TableCell>
                      <TableCell className="text-center text-amber-700">{c.excused}</TableCell>
                      <TableCell className="text-center text-red-700">{c.absent}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Podle lekce</CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Datum</TableHead>
                    <TableHead className="text-center">Přítomno</TableHead>
                    <TableHead className="text-center">Omluveno</TableHead>
                    <TableHead className="text-center">Nepřítomno</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {stats.lessons.map((l) => (
                    <TableRow key={l.date}>
                      <TableCell className="font-medium">
                        {new Date(l.date).toLocaleDateString("cs-CZ")}
                      </TableCell>
                      <TableCell className="text-center text-green-700">{l.present}</TableCell>
                      <TableCell className="text-center text-amber-700">{l.excused}</TableCell>
                      <TableCell className="text-center text-red-700">{l.absent}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}
