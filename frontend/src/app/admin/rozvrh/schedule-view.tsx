"use client";

import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState, useTransition } from "react";
import Link from "next/link";
import {
  ChevronLeft,
  ChevronRight,
  Users,
  MapPin,
  Clock,
  CalendarRange,
  Phone,
  Mail,
  User as UserIcon,
  EyeOff,
  Plus,
  CalendarX,
  CalendarClock,
  Trash2,
} from "lucide-react";
import type { Location, Program, ScheduleEntry } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Select } from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { cancelLesson, moveLesson, addOneOffLesson, deleteOverride } from "@/lib/actions";

const DAYS = ["Pondělí", "Úterý", "Středa", "Čtvrtek", "Pátek", "Sobota", "Neděle"];

// Horizontal grid: days are rows, time is the x-axis (7:00–20:00).
const START_HOUR = 7;
const END_HOUR = 20;
const TOTAL_MIN = (END_HOUR - START_HOUR) * 60;

const TYPE_STYLE: Record<
  string,
  { bar: string; bg: string; text: string; label: string }
> = {
  club: { bar: "bg-blue-500", bg: "bg-blue-50 hover:bg-blue-100", text: "text-blue-950", label: "Kroužek" },
  school: {
    bar: "bg-violet-500",
    bg: "bg-violet-50 hover:bg-violet-100",
    text: "text-violet-950",
    label: "Škola",
  },
};

function toMinutes(hhmm: string): number {
  const [h, m] = hhmm.split(":").map(Number);
  return h * 60 + m;
}

function addDays(iso: string, days: number): string {
  const d = new Date(iso + "T00:00:00Z");
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

function formatRange(weekStart: string): string {
  const start = new Date(weekStart + "T00:00:00Z");
  const end = new Date(start);
  end.setUTCDate(end.getUTCDate() + 6);
  const fmt = (d: Date) => `${d.getUTCDate()}. ${d.getUTCMonth() + 1}.`;
  return `${fmt(start)} – ${fmt(end)} ${end.getUTCFullYear()}`;
}

function formatDate(iso?: string | null): string {
  if (!iso) return "—";
  const d = new Date(iso + "T00:00:00Z");
  return `${d.getUTCDate()}. ${d.getUTCMonth() + 1}. ${d.getUTCFullYear()}`;
}

export function ScheduleView({
  entries,
  locations,
  programs,
  weekStart,
  currentLocation,
}: {
  entries: ScheduleEntry[];
  locations: Location[];
  programs: Program[];
  weekStart: string;
  currentLocation: string;
}) {
  const router = useRouter();
  const [detail, setDetail] = useState<ScheduleEntry | null>(null);
  const [isPending, startAction] = useTransition();
  const [moveFor, setMoveFor] = useState<ScheduleEntry | null>(null);
  const [oneOffOpen, setOneOffOpen] = useState(false);

  function runAction(fn: () => Promise<{ ok: boolean; error?: string }>, onDone?: () => void) {
    startAction(async () => {
      const res = await fn();
      if (!res.ok) {
        alert(res.error ?? "Akce selhala");
        return;
      }
      onDone?.();
      setDetail(null);
      router.refresh();
    });
  }

  function go(week: string, location = currentLocation) {
    const q = new URLSearchParams();
    q.set("week", week);
    if (location) q.set("location", location);
    router.push(`/admin/rozvrh?${q.toString()}`);
  }

  const byDay = useMemo(() => {
    const map: Record<number, ScheduleEntry[]> = {};
    for (const e of entries) {
      (map[e.weekday] ??= []).push(e);
    }
    return map;
  }, [entries]);

  // Assign overlapping lessons to horizontal lanes (stacked rows) so they don't cover each other.
  const laneByDay = useMemo(() => {
    const result: Record<number, { entry: ScheduleEntry; lane: number }[]> = {};
    const counts: Record<number, number> = {};
    for (const [wd, list] of Object.entries(byDay)) {
      const weekday = Number(wd);
      const sorted = [...list].sort((a, b) => toMinutes(a.startTime) - toMinutes(b.startTime));
      const laneEnds: number[] = []; // last end-minute per lane
      const placed = sorted.map((e) => {
        const start = toMinutes(e.startTime);
        const end = e.endTime ? toMinutes(e.endTime) : start + 45;
        let lane = laneEnds.findIndex((endMin) => endMin <= start);
        if (lane === -1) {
          lane = laneEnds.length;
          laneEnds.push(end);
        } else {
          laneEnds[lane] = end;
        }
        return { entry: e, lane };
      });
      result[weekday] = placed;
      counts[weekday] = Math.max(1, laneEnds.length);
    }
    return { result, counts };
  }, [byDay]);

  const hourMarks: number[] = [];
  for (let h = START_HOUR; h <= END_HOUR; h++) hourMarks.push(h);

  // Half-hour marks (for finer gridlines).
  const halfMarks: number[] = [];
  for (let m = START_HOUR * 60 + 30; m < END_HOUR * 60; m += 60) halfMarks.push(m);

  const leftPct = (min: number) => ((min - START_HOUR * 60) / TOTAL_MIN) * 100;
  const isEmptyWeek = entries.length === 0;

  const todayIso = new Date().toISOString().slice(0, 10);

  // Current-time indicator (updates every minute).
  const [nowMin, setNowMin] = useState<number | null>(null);
  useEffect(() => {
    const update = () => {
      const d = new Date();
      setNowMin(d.getHours() * 60 + d.getMinutes());
    };
    update();
    const id = setInterval(update, 60_000);
    return () => clearInterval(id);
  }, []);
  const nowInRange = nowMin != null && nowMin >= START_HOUR * 60 && nowMin <= END_HOUR * 60;

  return (
    <div className="flex h-full flex-col">
      {/* Toolbar */}
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <div className="flex items-center gap-2">
          <Button variant="outline" size="icon" onClick={() => go(addDays(weekStart, -7))} aria-label="Předchozí týden">
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <Button variant="outline" size="sm" onClick={() => go(new Date().toISOString().slice(0, 10))}>
            Dnešní týden
          </Button>
          <Button variant="outline" size="icon" onClick={() => go(addDays(weekStart, 7))} aria-label="Další týden">
            <ChevronRight className="h-4 w-4" />
          </Button>
        </div>
        <span className="text-sm font-semibold">{formatRange(weekStart)}</span>

        <div className="ml-auto flex items-center gap-3">
          <Legend />
          <Button size="sm" onClick={() => setOneOffOpen(true)}>
            <Plus className="h-4 w-4" /> Přidat akci
          </Button>
          <Select
            className=""
            value={currentLocation}
            onChange={(e) => go(weekStart, e.target.value)}
          >
            <option value="">Všechna místa</option>
            {locations.map((l) => (
              <option key={l.id} value={l.id}>
                {l.name}
              </option>
            ))}
          </Select>
        </div>
      </div>

      {isEmptyWeek && (
        <div className="mb-4 rounded-lg border border-dashed border-[var(--border)] bg-[var(--background)] p-4 text-center text-sm text-[var(--muted-foreground)]">
          V tomto týdnu nejsou naplánované žádné lekce.
        </div>
      )}

      {/* Horizontal week grid — days as rows, time on the x-axis. */}
      <div className="flex min-h-0 flex-1 flex-col overflow-hidden rounded-xl border border-[var(--border)] bg-[var(--background)] shadow-sm">
        {/* Time header */}
        <div className="flex border-b border-[var(--border)] bg-[var(--muted)]/40">
          <div className="flex w-16 shrink-0 items-center justify-center border-r border-[var(--border)] text-[10px] font-semibold uppercase tracking-wide text-[var(--muted-foreground)]">
            Den
          </div>
          <div className="relative h-8 flex-1">
            {hourMarks.map((h) => (
              <div
                key={h}
                className="absolute top-2 -translate-x-1/2 text-[11px] font-medium text-[var(--muted-foreground)]"
                style={{ left: `${leftPct(h * 60)}%` }}
              >
                {h}:00
              </div>
            ))}
          </div>
        </div>

        {/* Day rows */}
        <div className="flex min-h-0 flex-1 flex-col">
          {DAYS.map((day, i) => {
            const weekday = i + 1;
            const dayEntries = byDay[weekday] ?? [];
            const placed = laneByDay.result[weekday] ?? [];
            const laneCount = laneByDay.counts[weekday] ?? 1;
            const weekend = weekday >= 6;
            const dayIso = addDays(weekStart, i);
            const isToday = dayIso === todayIso;
            const dayDate = new Date(dayIso + "T00:00:00Z");
            return (
              <div
                key={day}
                style={{ flexGrow: laneCount }}
                className={`flex min-h-0 flex-1 border-b border-[var(--border)] last:border-b-0 ${
                  isToday ? "bg-blue-50/50" : weekend ? "bg-[var(--muted)]/30" : ""
                }`}
              >
                <div
                  className={`flex w-16 shrink-0 flex-col items-center justify-center border-r border-[var(--border)] ${
                    isToday ? "bg-blue-100/60" : ""
                  }`}
                >
                  <span className={`text-xs font-bold ${isToday ? "text-blue-700" : "text-[var(--foreground)]"}`}>
                    {day.slice(0, 2)}
                  </span>
                  <span className="text-[10px] text-[var(--muted-foreground)]">
                    {dayDate.getUTCDate()}. {dayDate.getUTCMonth() + 1}.
                  </span>
                </div>
                <div className="relative flex-1">
                  {/* half-hour gridlines (fainter) */}
                  {halfMarks.map((m) => (
                    <div
                      key={`h${m}`}
                      className="absolute inset-y-0 border-l border-dashed border-[var(--border)]/25"
                      style={{ left: `${leftPct(m)}%` }}
                    />
                  ))}
                  {/* hour gridlines */}
                  {hourMarks.map((h) => (
                    <div
                      key={h}
                      className="absolute inset-y-0 border-l border-[var(--border)]/40"
                      style={{ left: `${leftPct(h * 60)}%` }}
                    />
                  ))}
                  {/* current-time indicator (today only) */}
                  {isToday && nowInRange && (
                    <div
                      className="absolute inset-y-0 z-20 w-px bg-red-500"
                      style={{ left: `${leftPct(nowMin!)}%` }}
                    >
                      <span className="absolute -top-0.5 -left-[3px] h-1.5 w-1.5 rounded-full bg-red-500" />
                    </div>
                  )}
                  {/* empty-day hint */}
                  {dayEntries.length === 0 && (
                    <span className="absolute inset-0 flex items-center justify-center text-[11px] text-[var(--muted-foreground)]/50">
                      volno
                    </span>
                  )}
                  {/* lessons */}
                  {placed.map(({ entry: e, lane }, idx) => {
                    const start = toMinutes(e.startTime);
                    const end = e.endTime ? toMinutes(e.endTime) : start + 45;
                    const left = leftPct(start);
                    const width = ((end - start) / TOTAL_MIN) * 100;
                    const style = TYPE_STYLE[e.type] ?? TYPE_STYLE.club;
                    const full = e.capacity != null && e.spotsTaken >= e.capacity;
                    const hidden = e.status === "hidden";
                    // Ensure the block is wide enough to show the name; clamp so it stays in view.
                    const displayWidth = Math.min(Math.max(width, 12), 100 - left);
                    // Vertical lane packing: each overlapping lesson gets its own row within the day.
                    const laneHeight = 100 / laneCount;
                    const top = lane * laneHeight;
                    return (
                      <button
                        key={e.programId + idx}
                        onClick={() => setDetail(e)}
                        className={`group absolute flex overflow-hidden rounded-lg border text-left shadow-sm transition-all hover:z-10 hover:scale-[1.02] hover:shadow-md ${
                          hidden ? "border-dashed border-[var(--border)] bg-[var(--muted)]/60 opacity-70" : `border-black/5 ${style.bg}`
                        }`}
                        style={{
                          left: `${left}%`,
                          width: `${displayWidth}%`,
                          top: `calc(${top}% + 4px)`,
                          height: `calc(${laneHeight}% - 8px)`,
                        }}
                        title={`${e.programName}${hidden ? " (skrytý)" : ""} · ${e.startTime}${e.endTime ? "–" + e.endTime : ""}${
                          e.locationName ? " · " + e.locationName : ""
                        }${e.capacity != null ? ` · ${e.spotsTaken}/${e.capacity}` : ""}`}
                      >
                        <span className={`w-1.5 shrink-0 ${full ? "bg-red-500" : hidden ? "bg-[var(--muted-foreground)]/40" : style.bar}`} aria-hidden />
                        <span className={`flex min-w-0 flex-col justify-center gap-0.5 px-2 py-1 ${hidden ? "text-[var(--muted-foreground)]" : style.text}`}>
                          <span className="flex items-center gap-1 truncate text-xs font-semibold leading-tight">
                            {hidden && <EyeOff className="h-2.5 w-2.5 shrink-0" />}
                            <span className="truncate">{e.programName}</span>
                          </span>
                          <span className="flex items-center gap-1.5 text-[10px] font-medium opacity-75">
                            <span className="flex items-center gap-0.5 whitespace-nowrap">
                              <Clock className="h-2.5 w-2.5 shrink-0" />
                              {e.startTime}
                              {e.endTime ? `–${e.endTime}` : ""}
                            </span>
                            {e.capacity != null && (
                              <span className={`flex items-center gap-0.5 whitespace-nowrap ${full ? "font-bold text-red-700" : ""}`}>
                                <Users className="h-2.5 w-2.5" />
                                {e.spotsTaken}/{e.capacity}
                              </span>
                            )}
                          </span>
                          {e.locationName && (
                            <span className="flex items-center gap-0.5 truncate text-[10px] opacity-60">
                              <MapPin className="h-2.5 w-2.5 shrink-0" />
                              <span className="truncate">{e.locationName}</span>
                            </span>
                          )}
                        </span>
                      </button>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Lesson detail */}
      <Dialog open={detail !== null} onOpenChange={(o) => !o && setDetail(null)}>
        <DialogContent>
          {detail && (
            <>
              <DialogHeader>
                <DialogTitle className="flex items-center gap-2">
                  {detail.programName}
                  <Badge variant="info">{(TYPE_STYLE[detail.type] ?? TYPE_STYLE.club).label}</Badge>
                  {detail.overrideType === "one_off" && <Badge variant="warning">Jednorázová</Badge>}
                  {detail.overrideType === "moved" && <Badge variant="warning">Přesunuto</Badge>}
                  {detail.status === "hidden" && <Badge variant="default">Skrytý</Badge>}
                </DialogTitle>
              </DialogHeader>

              <div className="grid gap-3 text-sm">
                <Row icon={Clock} label="Čas">
                  {DAYS[detail.weekday - 1]} {detail.startTime}
                  {detail.endTime ? `–${detail.endTime}` : ""}
                  {detail.durationMin ? ` (${detail.durationMin} min)` : ""}
                </Row>
                <Row icon={CalendarRange} label="Období">
                  {formatDate(detail.validFrom)} – {formatDate(detail.validTo)}
                </Row>
                <Row icon={Users} label="Obsazenost">
                  {detail.capacity != null
                    ? `${detail.spotsTaken} / ${detail.capacity} dětí`
                    : `${detail.spotsTaken} dětí (bez limitu)`}
                </Row>
                {detail.locationName && (
                  <Row icon={MapPin} label="Místo">
                    {detail.locationName}
                    {detail.locationAddress ? `, ${detail.locationAddress}` : ""}
                  </Row>
                )}

                {(detail.contactName || detail.contactPhone || detail.contactEmail) && (
                  <div className="rounded-md border border-[var(--border)] p-3">
                    <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-[var(--muted-foreground)]">
                      Kontaktní osoba
                    </p>
                    <div className="grid gap-1.5">
                      {detail.contactName && (
                        <Row icon={UserIcon} label="">
                          {detail.contactName}
                        </Row>
                      )}
                      {detail.contactPhone && (
                        <Row icon={Phone} label="">
                          <a href={`tel:${detail.contactPhone}`} className="hover:underline">
                            {detail.contactPhone}
                          </a>
                        </Row>
                      )}
                      {detail.contactEmail && (
                        <Row icon={Mail} label="">
                          <a href={`mailto:${detail.contactEmail}`} className="hover:underline">
                            {detail.contactEmail}
                          </a>
                        </Row>
                      )}
                    </div>
                  </div>
                )}
              </div>

              <div className="mt-4 flex flex-wrap justify-end gap-2 border-t border-[var(--border)] pt-4">
                {detail.overrideId ? (
                  // One-off or moved lesson: allow removing the override.
                  <Button
                    variant="outline"
                    size="sm"
                    className="text-[var(--destructive)]"
                    disabled={isPending}
                    onClick={() => {
                      if (confirm("Opravdu odstranit tuto úpravu termínu?"))
                        runAction(() => deleteOverride(detail.overrideId!));
                    }}
                  >
                    <Trash2 className="h-4 w-4" /> Odebrat
                  </Button>
                ) : (
                  // Regular recurring occurrence: cancel or move this specific date.
                  <>
                    <Button
                      variant="outline"
                      size="sm"
                      className="text-[var(--destructive)]"
                      disabled={isPending}
                      onClick={() => {
                        if (confirm(`Zrušit termín ${formatDate(detail.date)} (${detail.programName})? Např. svátek nebo zavřeno.`))
                          runAction(() => cancelLesson(detail.programId, detail.date, "zrušeno"));
                      }}
                    >
                      <CalendarX className="h-4 w-4" /> Zrušit termín
                    </Button>
                    <Button variant="outline" size="sm" disabled={isPending} onClick={() => setMoveFor(detail)}>
                      <CalendarClock className="h-4 w-4" /> Přesunout
                    </Button>
                  </>
                )}
                {detail.programId && (
                  <Button variant="outline" size="sm" asChild>
                    <Link href={`/admin/registrace?program=${detail.programId}`}>Registrace</Link>
                  </Button>
                )}
              </div>
            </>
          )}
        </DialogContent>
      </Dialog>

      {/* Move a recurring occurrence */}
      <MoveDialog
        entry={moveFor}
        locations={locations}
        onClose={() => setMoveFor(null)}
        onSubmit={(body) => runAction(() => moveLesson(body), () => setMoveFor(null))}
        pending={isPending}
      />

      {/* Add a one-off lesson/event */}
      <OneOffDialog
        open={oneOffOpen}
        programs={programs}
        locations={locations}
        defaultLocation={currentLocation}
        onClose={() => setOneOffOpen(false)}
        onSubmit={(body) => runAction(() => addOneOffLesson(body), () => setOneOffOpen(false))}
        pending={isPending}
      />
    </div>
  );
}

function Legend() {
  return (
    <div className="hidden items-center gap-3 text-xs text-[var(--muted-foreground)] md:flex">
      <span className="flex items-center gap-1">
        <span className="h-2.5 w-2.5 rounded-sm bg-blue-500" /> Kroužek
      </span>
      <span className="flex items-center gap-1">
        <span className="h-2.5 w-2.5 rounded-sm bg-violet-500" /> Škola
      </span>
      <span className="flex items-center gap-1">
        <span className="h-2.5 w-2.5 rounded-sm bg-red-500" /> Plno
      </span>
    </div>
  );
}

function Row({
  icon: Icon,
  label,
  children,
}: {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  children: React.ReactNode;
}) {
  return (
    <div className="flex items-start gap-2">
      <Icon className="mt-0.5 h-4 w-4 shrink-0 text-[var(--muted-foreground)]" />
      <div>
        {label && <span className="text-[var(--muted-foreground)]">{label}: </span>}
        <span className="font-medium">{children}</span>
      </div>
    </div>
  );
}

function MoveDialog({
  entry,
  locations,
  onClose,
  onSubmit,
  pending,
}: {
  entry: ScheduleEntry | null;
  locations: Location[];
  onClose: () => void;
  onSubmit: (body: {
    programId: string;
    originalDate: string;
    newDate: string;
    newTime: string;
    durationMin?: number | null;
    locationId?: string | null;
  }) => void;
  pending: boolean;
}) {
  const [newDate, setNewDate] = useState("");
  const [newTime, setNewTime] = useState("");
  const [locationId, setLocationId] = useState("");

  useEffect(() => {
    if (entry) {
      setNewDate(entry.date);
      setNewTime(entry.startTime ?? "");
      setLocationId(entry.locationId ?? "");
    }
  }, [entry]);

  return (
    <Dialog open={entry !== null} onOpenChange={(o) => !o && onClose()}>
      <DialogContent>
        {entry && (
          <>
            <DialogHeader>
              <DialogTitle>Přesunout termín</DialogTitle>
            </DialogHeader>
            <p className="mb-3 text-sm text-[var(--muted-foreground)]">
              {entry.programName} — původně {formatDate(entry.date)} {entry.startTime}
            </p>
            <div className="grid gap-3">
              <div className="grid grid-cols-2 gap-3">
                <div className="flex flex-col gap-1.5">
                  <Label>Nové datum</Label>
                  <Input type="date" value={newDate} onChange={(e) => setNewDate(e.target.value)} />
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label>Nový čas</Label>
                  <Input type="time" value={newTime} onChange={(e) => setNewTime(e.target.value)} />
                </div>
              </div>
              <div className="flex flex-col gap-1.5">
                <Label>Místo (nepovinné)</Label>
                <Select value={locationId} onChange={(e) => setLocationId(e.target.value)}>
                  <option value="">Beze změny</option>
                  {locations.map((l) => (
                    <option key={l.id} value={l.id}>
                      {l.name}
                    </option>
                  ))}
                </Select>
              </div>
              <div className="mt-2 flex justify-end gap-2">
                <Button variant="outline" onClick={onClose} disabled={pending}>
                  Zrušit
                </Button>
                <Button
                  disabled={pending || !newDate || !newTime}
                  onClick={() =>
                    onSubmit({
                      programId: entry.programId,
                      originalDate: entry.date,
                      newDate,
                      newTime,
                      durationMin: entry.durationMin ?? null,
                      locationId: locationId || null,
                    })
                  }
                >
                  Přesunout
                </Button>
              </div>
            </div>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}

function OneOffDialog({
  open,
  programs,
  locations,
  defaultLocation,
  onClose,
  onSubmit,
  pending,
}: {
  open: boolean;
  programs: Program[];
  locations: Location[];
  defaultLocation: string;
  onClose: () => void;
  onSubmit: (body: {
    programId?: string | null;
    title?: string | null;
    date: string;
    time: string;
    durationMin?: number | null;
    locationId?: string | null;
  }) => void;
  pending: boolean;
}) {
  const [programId, setProgramId] = useState("");
  const [title, setTitle] = useState("");
  const [date, setDate] = useState("");
  const [time, setTime] = useState("");
  const [duration, setDuration] = useState("");
  const [locationId, setLocationId] = useState(defaultLocation);

  useEffect(() => {
    if (open) {
      setProgramId("");
      setTitle("");
      setDate("");
      setTime("");
      setDuration("");
      setLocationId(defaultLocation);
    }
  }, [open, defaultLocation]);

  const recurring = programs.filter((p) => p.weekday && p.time);

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Přidat jednorázovou akci</DialogTitle>
        </DialogHeader>
        <div className="grid gap-3">
          <div className="flex flex-col gap-1.5">
            <Label>Program (náhradní lekce) — nebo nech prázdné pro vlastní akci</Label>
            <Select value={programId} onChange={(e) => setProgramId(e.target.value)}>
              <option value="">— vlastní akce (název níže) —</option>
              {recurring.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                </option>
              ))}
            </Select>
          </div>
          {!programId && (
            <div className="flex flex-col gap-1.5">
              <Label>Název akce</Label>
              <Input
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Ukázková hodina pro rodiče"
              />
            </div>
          )}
          <div className="grid grid-cols-3 gap-3">
            <div className="flex flex-col gap-1.5">
              <Label>Datum</Label>
              <Input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label>Čas</Label>
              <Input type="time" value={time} onChange={(e) => setTime(e.target.value)} />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label>Délka (min)</Label>
              <Input type="number" value={duration} onChange={(e) => setDuration(e.target.value)} placeholder="60" />
            </div>
          </div>
          <div className="flex flex-col gap-1.5">
            <Label>Místo (nepovinné)</Label>
            <Select value={locationId} onChange={(e) => setLocationId(e.target.value)}>
              <option value="">Bez místa</option>
              {locations.map((l) => (
                <option key={l.id} value={l.id}>
                  {l.name}
                </option>
              ))}
            </Select>
          </div>
          <div className="mt-2 flex justify-end gap-2">
            <Button variant="outline" onClick={onClose} disabled={pending}>
              Zrušit
            </Button>
            <Button
              disabled={pending || !date || !time || (!programId && !title.trim())}
              onClick={() =>
                onSubmit({
                  programId: programId || null,
                  title: programId ? null : title,
                  date,
                  time,
                  durationMin: duration ? Number(duration) : null,
                  locationId: locationId || null,
                })
              }
            >
              Přidat
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
