"use client";

import { useState } from "react";
import { Camera, CameraOff, ChevronDown, ChevronRight, MapPin } from "lucide-react";
import type { MediaConsentGroup } from "@/lib/types";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";

export function ConsentsView({ groups }: { groups: MediaConsentGroup[] }) {
  const [q, setQ] = useState("");
  const needle = q.trim().toLocaleLowerCase("cs");
  const visible = needle
    ? groups.filter(
        (g) =>
          g.programName.toLocaleLowerCase("cs").includes(needle) ||
          (g.locationName ?? "").toLocaleLowerCase("cs").includes(needle),
      )
    : groups;

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center gap-3">
        <Input
          className="w-full sm:w-72"
          placeholder="Hledat kroužek nebo školku…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
        <span className="flex items-center gap-3 text-sm text-[var(--muted-foreground)]">
          <span className="flex items-center gap-1 text-success"><Camera className="h-4 w-4" /> smí se fotit</span>
          <span className="flex items-center gap-1 text-danger"><CameraOff className="h-4 w-4" /> nefotit / nezveřejňovat</span>
        </span>
      </div>
      {visible.map((g, i) => (
        <Group key={g.programId} group={g} tint={i % 2 === 0 ? "a" : "b"} />
      ))}
    </div>
  );
}

function Group({ group, tint }: { group: MediaConsentGroup; tint: "a" | "b" }) {
  const [open, setOpen] = useState(false);
  const without = group.children.length - group.consentCount;
  return (
    <div
      className="overflow-hidden rounded-lg border-2"
      style={{ background: `var(--group-${tint}-bg)`, borderColor: `var(--group-${tint}-border)` }}
    >
      <button onClick={() => setOpen((o) => !o)} className="flex w-full items-center gap-3 px-4 py-3 text-left">
        {open ? <ChevronDown className="h-4 w-4 shrink-0" /> : <ChevronRight className="h-4 w-4 shrink-0" />}
        <span className="flex min-w-0 flex-1 flex-col">
          <span className="truncate font-semibold leading-tight">{group.programName}</span>
          {group.locationName && (
            <span className="flex items-center gap-1 truncate text-xs text-[var(--muted-foreground)]">
              <MapPin className="h-3 w-3" /> {group.locationName}
              {group.locationAddress ? `, ${group.locationAddress}` : ""}
            </span>
          )}
        </span>
        <span className="shrink-0 text-sm tabular-nums text-[var(--muted-foreground)]">
          {group.children.length} dětí
        </span>
        {without > 0 ? (
          <Badge variant="danger" className="shrink-0">{without}× bez souhlasu</Badge>
        ) : (
          <Badge variant="success" className="shrink-0">všichni souhlasí</Badge>
        )}
      </button>
      {open && (
        <ul className="divide-y divide-[var(--border)] border-t border-[var(--border)] bg-[var(--background)]">
          {group.children.map((c, idx) => (
            <li key={idx} className="flex items-center justify-between gap-3 px-4 py-2 text-sm">
              <span className="font-medium">{c.name}</span>
              {c.consentMedia ? (
                <span className="flex items-center gap-1 text-success"><Camera className="h-4 w-4" /> souhlas</span>
              ) : (
                <span className="flex items-center gap-1 font-semibold text-danger"><CameraOff className="h-4 w-4" /> NEFOTIT</span>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
