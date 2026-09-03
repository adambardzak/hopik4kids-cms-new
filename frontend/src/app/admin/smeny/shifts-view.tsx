"use client";

import { useRouter } from "next/navigation";
import { useMemo, useState, useTransition } from "react";
import { CalendarPlus, Check, X, Clock, MapPin, Users2 } from "lucide-react";
import type { ShiftSlot } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { signupShift, cancelShift, approveShift, rejectShift } from "@/lib/actions";
import { EmptyState } from "@/components/page-header";
import { useToast } from "@/components/ui/toast";

const WEEKDAYS = ["Ne", "Po", "Út", "St", "Čt", "Pá", "So"];

function fmtDate(iso: string) {
  const d = new Date(iso + "T00:00:00");
  return `${WEEKDAYS[d.getDay()]} ${d.getDate()}. ${d.getMonth() + 1}.`;
}

function statusBadge(status?: string | null) {
  if (status === "APPROVED") return <Badge variant="success">Potvrzeno</Badge>;
  if (status === "PENDING") return <Badge variant="warning">Čeká na schválení</Badge>;
  return null;
}

export function ShiftsView({
  open,
  mine,
  canApprove,
}: {
  open: ShiftSlot[];
  mine: ShiftSlot[];
  canApprove: boolean;
}) {
  const router = useRouter();
  const toast = useToast();
  const [tab, setTab] = useState<"open" | "mine">("open");
  const [isPending, start] = useTransition();
  const [busyKey, setBusyKey] = useState<string | null>(null);

  const slots = tab === "open" ? open : mine;

  const byDate = useMemo(() => {
    const map = new Map<string, ShiftSlot[]>();
    for (const s of slots) {
      (map.get(s.date) ?? map.set(s.date, []).get(s.date)!).push(s);
    }
    return [...map.entries()].sort(([a], [b]) => a.localeCompare(b));
  }, [slots]);

  function act(key: string, fn: () => Promise<{ ok: boolean; error?: string }>) {
    setBusyKey(key);
    start(async () => {
      const res = await fn();
      setBusyKey(null);
      if (!res.ok) {
        toast.error(res.error ?? "Akce selhala");
        return;
      }
      router.refresh();
    });
  }

  return (
    <div>
      <div className="mb-4 inline-flex rounded-lg border border-[var(--border)] bg-[var(--background)] p-1">
        <button
          onClick={() => setTab("open")}
          className={cn(
            "rounded-md px-4 py-1.5 text-sm font-medium transition-colors",
            tab === "open" ? "" : "text-[var(--muted-foreground)] hover:bg-[var(--muted)] hover:text-[var(--foreground)]",
          )}
          style={tab === "open" ? { background: "var(--accent)", color: "var(--accent-fg)" } : undefined}
        >
          Volné hodiny
        </button>
        <button
          onClick={() => setTab("mine")}
          className={cn(
            "rounded-md px-4 py-1.5 text-sm font-medium transition-colors",
            tab === "mine" ? "" : "text-[var(--muted-foreground)] hover:bg-[var(--muted)] hover:text-[var(--foreground)]",
          )}
          style={tab === "mine" ? { background: "var(--accent)", color: "var(--accent-fg)" } : undefined}
        >
          Moje hodiny {mine.length > 0 && <span className="ml-1 opacity-70">({mine.length})</span>}
        </button>
      </div>

      {byDate.length === 0 ? (
        <EmptyState
          icon={CalendarPlus}
          message={tab === "open" ? "Žádné hodiny v příštích 6 týdnech." : "Zatím nejsi přihlášený/á na žádnou hodinu."}
        />
      ) : (
        <div className="space-y-6">
          {byDate.map(([date, daySlots]) => (
            <div key={date}>
              <h3 className="mb-2 text-sm font-semibold uppercase tracking-wide text-[var(--muted-foreground)]">
                {fmtDate(date)}
              </h3>
              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {daySlots.map((s) => {
                  const key = `${s.programId}|${s.date}`;
                  const full = s.approvedCount >= s.trainersNeeded;
                  const busy = busyKey === key || (s.mySignupId ? busyKey === s.mySignupId : false);
                  return (
                    <div key={key} className="rounded-lg border border-[var(--border)] bg-[var(--background)] p-4">
                      <div className="mb-1 flex items-start justify-between gap-2">
                        <p className="font-medium leading-tight">{s.programName}</p>
                        {statusBadge(s.myStatus)}
                      </div>
                      <div className="space-y-1 text-sm text-[var(--muted-foreground)]">
                        {s.startTime && (
                          <p className="flex items-center gap-1.5">
                            <Clock className="h-3.5 w-3.5" />
                            {s.startTime}
                            {s.endTime ? `–${s.endTime}` : ""}
                          </p>
                        )}
                        {s.locationName && (
                          <p className="flex items-center gap-1.5">
                            <MapPin className="h-3.5 w-3.5" />
                            {s.locationName}
                          </p>
                        )}
                        <p className="flex items-center gap-1.5">
                          <Users2 className="h-3.5 w-3.5" />
                          {s.approvedCount}/{s.trainersNeeded} trenérů
                          {s.pendingCount > 0 && ` (+${s.pendingCount} čeká)`}
                        </p>
                      </div>

                      {/* Signed-up trainers (visible to admins for approval) */}
                      {canApprove && s.signups.length > 0 && (
                        <div className="mt-3 space-y-1.5 border-t border-[var(--border)] pt-3">
                          {s.signups.map((p) => (
                            <div key={p.signupId} className="flex items-center justify-between gap-2 text-sm">
                              <span className="flex items-center gap-1.5">
                                {p.trainerName}
                                {p.status === "APPROVED" && <Check className="h-3.5 w-3.5 text-[var(--success,green)]" />}
                              </span>
                              {p.status === "PENDING" && (
                                <span className="flex gap-1">
                                  <button
                                    title="Schválit"
                                    disabled={isPending}
                                    onClick={() => act(p.signupId, () => approveShift(p.signupId))}
                                    className="rounded p-1 text-[var(--success,green)] hover:bg-[var(--muted)]"
                                  >
                                    <Check className="h-4 w-4" />
                                  </button>
                                  <button
                                    title="Zamítnout"
                                    disabled={isPending}
                                    onClick={() => act(p.signupId, () => rejectShift(p.signupId))}
                                    className="rounded p-1 text-[var(--destructive)] hover:bg-[var(--muted)]"
                                  >
                                    <X className="h-4 w-4" />
                                  </button>
                                </span>
                              )}
                            </div>
                          ))}
                        </div>
                      )}

                      <div className="mt-3">
                        {s.mySignupId ? (
                          <Button
                            variant="outline"
                            className="w-full"
                            disabled={busy}
                            onClick={() => act(s.mySignupId!, () => cancelShift(s.mySignupId!))}
                          >
                            Zrušit přihlášení
                          </Button>
                        ) : (
                          <Button
                            className="w-full"
                            disabled={busy || full}
                            onClick={() => act(key, () => signupShift(s.programId, s.date))}
                          >
                            <CalendarPlus className="h-4 w-4" />
                            {full ? "Obsazeno" : "Přihlásit se"}
                          </Button>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
