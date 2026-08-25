"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { Plus, Pencil, Trash2 } from "lucide-react";
import type { Location, Program, Trainer } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Select } from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { IconAction } from "@/components/ui/icon-action";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { deleteProgram, saveProgram } from "@/lib/actions";

const WEEKDAYS = ["", "Pondělí", "Úterý", "Středa", "Čtvrtek", "Pátek", "Sobota", "Neděle"];
const TYPE_LABELS: Record<string, string> = { club: "Kroužek", school: "Škola", camp: "Kemp" };
const STATUS_LABELS: Record<string, { label: string; variant: "success" | "default" | "warning" }> = {
  active: { label: "Aktivní", variant: "success" },
  hidden: { label: "Skrytý", variant: "default" },
  archived: { label: "Archiv", variant: "warning" },
};

type FormState = Partial<Program> & { accessCode?: string };

export function ProgramsManager({
  programs,
  locations,
  trainers,
  openCreateOnly,
}: {
  programs: Program[];
  locations: Location[];
  trainers: Trainer[];
  openCreateOnly?: boolean;
}) {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<FormState>({});
  const [error, setError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  function openCreate() {
    setForm({ type: "club", status: "active", shirtPolicy: "optional", accessMode: "public", price: 0 });
    setError(null);
    setOpen(true);
  }

  function openEdit(p: Program) {
    setForm({ ...p });
    setError(null);
    setOpen(true);
  }

  function set<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  function submit() {
    setError(null);
    const isCamp = form.type === "camp";
    const body = {
      type: form.type,
      name: form.name,
      price: Number(form.price ?? 0),
      capacity: form.capacity ? Number(form.capacity) : null,
      status: form.status,
      shirtPolicy: form.shirtPolicy,
      accessMode: form.accessMode,
      restrictionNote: form.restrictionNote || null,
      accessCode: form.accessCode || null,
      locationId: form.locationId || null,
      weekday: !isCamp && form.weekday ? Number(form.weekday) : null,
      time: !isCamp ? form.time || null : null,
      schoolPart: form.type === "school" ? form.schoolPart || null : null,
      durationMin: !isCamp && form.durationMin ? Number(form.durationMin) : null,
      trainersNeeded: !isCamp ? Number(form.trainersNeeded ?? 1) : null,
      validFrom: !isCamp ? form.validFrom || null : null,
      validTo: !isCamp ? form.validTo || null : null,
      startDate: isCamp ? form.startDate || null : null,
      endDate: isCamp ? form.endDate || null : null,
      trainerIds: form.trainerIds ?? [],
    };
    startTransition(async () => {
      const res = await saveProgram(form.id ?? null, body);
      if (!res.ok) {
        setError(res.error ?? "Uložení selhalo");
        return;
      }
      setOpen(false);
      router.refresh();
    });
  }

  function remove(p: Program) {
    if (!confirm(`Opravdu smazat program „${p.name}"?`)) return;
    startTransition(async () => {
      const res = await deleteProgram(p.id);
      if (!res.ok) alert(res.error ?? "Program nelze smazat (může mít registrace).");
      router.refresh();
    });
  }

  const isCamp = form.type === "camp";

  return (
    <div>
      <div className="mb-4 flex justify-end">
        <Button onClick={openCreate}>
          <Plus className="h-4 w-4" /> Přidat program
        </Button>
      </div>

      {!openCreateOnly && (
        <div className="rounded-lg border border-[var(--border)] bg-[var(--background)]">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Název</TableHead>
                <TableHead>Typ</TableHead>
                <TableHead>Termín</TableHead>
                <TableHead>Cena</TableHead>
                <TableHead>Obsazenost</TableHead>
                <TableHead>Stav</TableHead>
                <TableHead></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {programs.map((p) => {
                const st = STATUS_LABELS[p.status] ?? STATUS_LABELS.active;
                const full = p.capacity != null && p.spotsTaken >= p.capacity;
                return (
                  <TableRow key={p.id}>
                    <TableCell className="font-medium">{p.name}</TableCell>
                    <TableCell>{TYPE_LABELS[p.type]}</TableCell>
                    <TableCell className="text-sm">
                      {p.type === "camp"
                        ? `${p.startDate ?? "?"} – ${p.endDate ?? "?"}`
                        : `${WEEKDAYS[p.weekday ?? 0] || ""} ${p.time ?? ""}`.trim() || "—"}
                    </TableCell>
                    <TableCell>{p.price} Kč</TableCell>
                    <TableCell>
                      <span className={full ? "font-semibold text-[var(--destructive)]" : ""}>
                        {p.spotsTaken}
                        {p.capacity != null ? ` / ${p.capacity}` : ""}
                      </span>
                    </TableCell>
                    <TableCell>
                      <Badge variant={st.variant}>{st.label}</Badge>
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-1">
                        <IconAction label="Upravit" icon={Pencil} onClick={() => openEdit(p)} />
                        <IconAction
                          label="Smazat"
                          icon={Trash2}
                          onClick={() => remove(p)}
                          className="text-[var(--destructive)]"
                        />
                      </div>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </div>
      )}

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-h-[85vh] overflow-auto">
          <DialogHeader>
            <DialogTitle>{form.id ? "Upravit program" : "Nový program"}</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4">
            <div className="grid grid-cols-2 gap-4">
              <Field label="Typ">
                <Select
                  className="w-full"
                  value={form.type ?? "club"}
                  onChange={(e) => set("type", e.target.value as Program["type"])}
                >
                  <option value="club">Kroužek</option>
                  <option value="school">Cvičení ve škole</option>
                  <option value="camp">Kemp</option>
                </Select>
              </Field>
              <Field label="Stav">
                <Select
                  className="w-full"
                  value={form.status ?? "active"}
                  onChange={(e) => set("status", e.target.value as Program["status"])}
                >
                  <option value="active">Aktivní</option>
                  <option value="hidden">Skrytý</option>
                  <option value="archived">Archivovaný</option>
                </Select>
              </Field>
            </div>

            <Field label="Název">
              <Input value={form.name ?? ""} onChange={(e) => set("name", e.target.value)} />
            </Field>

            <div className="grid grid-cols-2 gap-4">
              <Field label="Cena (Kč)">
                <Input
                  type="number"
                  value={form.price ?? 0}
                  onChange={(e) => set("price", Number(e.target.value))}
                />
              </Field>
              <Field label="Kapacita (prázdné = neomezeno)">
                <Input
                  type="number"
                  value={form.capacity ?? ""}
                  onChange={(e) => set("capacity", e.target.value ? Number(e.target.value) : null)}
                />
              </Field>
            </div>

            <Field label="Místo">
              <Select
                className="w-full"
                value={form.locationId ?? ""}
                onChange={(e) => set("locationId", e.target.value || null)}
              >
                <option value="">— žádné —</option>
                {locations.map((l) => (
                  <option key={l.id} value={l.id}>
                    {l.name}
                  </option>
                ))}
              </Select>
            </Field>

            {!isCamp ? (
              <>
                <div className="grid grid-cols-3 gap-4">
                  <Field label="Den v týdnu">
                    <Select
                      className="w-full"
                      value={form.weekday ?? ""}
                      onChange={(e) => set("weekday", e.target.value ? Number(e.target.value) : null)}
                    >
                      <option value="">—</option>
                      {WEEKDAYS.slice(1).map((d, i) => (
                        <option key={i + 1} value={i + 1}>
                          {d}
                        </option>
                      ))}
                    </Select>
                  </Field>
                  <Field label="Čas (HH:MM)">
                    <Input value={form.time ?? ""} placeholder="16:00" onChange={(e) => set("time", e.target.value)} />
                  </Field>
                  <Field label="Délka (min)">
                    <Input
                      type="number"
                      value={form.durationMin ?? ""}
                      onChange={(e) => set("durationMin", e.target.value ? Number(e.target.value) : null)}
                    />
                  </Field>
                  <Field label="Počet trenérů na hodinu">
                    <Input
                      type="number"
                      min={1}
                      value={form.trainersNeeded ?? 1}
                      onChange={(e) => set("trainersNeeded", e.target.value ? Number(e.target.value) : 1)}
                    />
                  </Field>
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <Field label="Období od (první lekce)">
                    <Input
                      type="date"
                      value={form.validFrom ?? ""}
                      onChange={(e) => set("validFrom", e.target.value)}
                    />
                  </Field>
                  <Field label="Období do (poslední lekce)">
                    <Input
                      type="date"
                      value={form.validTo ?? ""}
                      onChange={(e) => set("validTo", e.target.value)}
                    />
                  </Field>
                </div>
                <p className="-mt-2 text-xs text-[var(--muted-foreground)]">
                  Kroužek se v rozvrhu zobrazuje jen v tomto období. Prázdné = bez omezení.
                </p>
              </>
            ) : (
              <div className="grid grid-cols-2 gap-4">
                <Field label="Od">
                  <Input type="date" value={form.startDate ?? ""} onChange={(e) => set("startDate", e.target.value)} />
                </Field>
                <Field label="Do">
                  <Input type="date" value={form.endDate ?? ""} onChange={(e) => set("endDate", e.target.value)} />
                </Field>
              </div>
            )}

            {form.type === "school" && (
              <Field label="Část dne">
                <Select
                  className="w-full"
                  value={form.schoolPart ?? ""}
                  onChange={(e) => set("schoolPart", (e.target.value || null) as Program["schoolPart"])}
                >
                  <option value="">—</option>
                  <option value="morning">Dopoledne</option>
                  <option value="afternoon">Odpoledne</option>
                </Select>
              </Field>
            )}

            <div className="grid grid-cols-2 gap-4">
              <Field label="Dres">
                <Select
                  className="w-full"
                  value={form.shirtPolicy ?? "none"}
                  onChange={(e) => set("shirtPolicy", e.target.value as Program["shirtPolicy"])}
                >
                  <option value="none">Bez dresu</option>
                  <option value="optional">Volitelný (+500 Kč)</option>
                  <option value="required">Povinný</option>
                </Select>
              </Field>
              <Field label="Přístup">
                <Select
                  className="w-full"
                  value={form.accessMode ?? "public"}
                  onChange={(e) => set("accessMode", e.target.value as Program["accessMode"])}
                >
                  <option value="public">Veřejný</option>
                  <option value="notice_only">Jen upozornění</option>
                  <option value="code">Přístupový kód</option>
                  <option value="unlisted">Nelistovaný</option>
                </Select>
              </Field>
            </div>

            {(form.accessMode === "notice_only" || form.accessMode === "code") && (
              <Field label="Text pro rodiče (upozornění)">
                <Input
                  value={form.restrictionNote ?? ""}
                  placeholder="Pouze pro děti z MŠ Chudenice"
                  onChange={(e) => set("restrictionNote", e.target.value)}
                />
              </Field>
            )}
            {form.accessMode === "code" && (
              <Field label={form.hasAccessCode ? "Nový přístupový kód (prázdné = beze změny)" : "Přístupový kód"}>
                <Input value={form.accessCode ?? ""} onChange={(e) => set("accessCode", e.target.value)} />
              </Field>
            )}

            {error && <p className="text-sm text-[var(--destructive)]">{error}</p>}

            {/* Trainer assignment — trainers see only their assigned programs (rozvrh/docházka). */}
            <Field label="Trenéři">
              {trainers.length === 0 ? (
                <p className="text-xs text-[var(--muted-foreground)]">
                  Zatím žádní trenéři. Pozvi je v sekci Tým (role Trenér).
                </p>
              ) : (
                <div className="grid grid-cols-2 gap-2">
                  {trainers.map((t) => {
                    const checked = (form.trainerIds ?? []).includes(t.id);
                    return (
                      <label key={t.id} className="flex items-center gap-2 text-sm">
                        <input
                          type="checkbox"
                          checked={checked}
                          onChange={(e) => {
                            const cur = new Set(form.trainerIds ?? []);
                            if (e.target.checked) cur.add(t.id);
                            else cur.delete(t.id);
                            set("trainerIds", Array.from(cur));
                          }}
                        />
                        {t.name}
                      </label>
                    );
                  })}
                </div>
              )}
            </Field>

            <div className="flex justify-end gap-2">
              <Button variant="outline" onClick={() => setOpen(false)} disabled={isPending}>
                Zrušit
              </Button>
              <Button onClick={submit} disabled={isPending}>
                {isPending ? "Ukládám…" : "Uložit"}
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
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
