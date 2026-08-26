"use client";

import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
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
} from "lucide-react";
import type { Location, ScheduleEntry } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Select } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";

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
  weekStart,
  currentLocation,
}: {
  entries: ScheduleEntry[];
  locations: Location[];
  weekStart: string;
  currentLocation: string;
}) {
  const router = useRouter();
  const [detail, setDetail] = useState<ScheduleEntry | null>(null);

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
                    // Ensure the block is wide enough to show the name; clamp so it stays in view.
                    const displayWidth = Math.min(Math.max(width, 12), 100 - left);
                    // Vertical lane packing: each overlapping lesson gets its own row within the day.
                    const laneHeight = 100 / laneCount;
                    const top = lane * laneHeight;
                    return (
                      <button
                        key={e.programId + idx}
                        onClick={() => setDetail(e)}
                        className={`group absolute flex overflow-hidden rounded-lg border border-black/5 text-left shadow-sm transition-all hover:z-10 hover:scale-[1.02] hover:shadow-md ${style.bg}`}
                        style={{
                          left: `${left}%`,
                          width: `${displayWidth}%`,
                          top: `calc(${top}% + 4px)`,
                          height: `calc(${laneHeight}% - 8px)`,
                        }}
                        title={`${e.programName} · ${e.startTime}${e.endTime ? "–" + e.endTime : ""}${
                          e.locationName ? " · " + e.locationName : ""
                        }${e.capacity != null ? ` · ${e.spotsTaken}/${e.capacity}` : ""}`}
                      >
                        <span className={`w-1.5 shrink-0 ${full ? "bg-red-500" : style.bar}`} aria-hidden />
                        <span className={`flex min-w-0 flex-col justify-center gap-0.5 px-2 py-1 ${style.text}`}>
                          <span className="truncate text-xs font-semibold leading-tight">
                            {e.programName}
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

              <div className="mt-4 flex justify-end gap-2 border-t border-[var(--border)] pt-4">
                <Button variant="outline" size="sm" asChild>
                  <Link href={`/admin/registrace?program=${detail.programId}`}>Registrace</Link>
                </Button>
                <Button size="sm" asChild>
                  <Link href="/admin/programy">Upravit program</Link>
                </Button>
              </div>
            </>
          )}
        </DialogContent>
      </Dialog>
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
