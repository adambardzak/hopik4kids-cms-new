"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { Plus, Pencil, Trash2 } from "lucide-react";
import type { Location } from "@/lib/types";
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
import { deleteLocation, saveLocation } from "@/lib/actions";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm";

const KIND_LABELS: Record<string, string> = {
  kindergarten: "Školka",
  school: "Škola",
  venue: "Sportoviště",
};

type FormState = Partial<Location>;

export function LocationsManager({
  locations,
  openCreateOnly,
}: {
  locations: Location[];
  openCreateOnly?: boolean;
}) {
  const router = useRouter();
  const toast = useToast();
  const confirm = useConfirm();
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<FormState>({});
  const [error, setError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  function openCreate() {
    setForm({ kind: "kindergarten" });
    setError(null);
    setOpen(true);
  }

  function openEdit(l: Location) {
    setForm({ ...l });
    setError(null);
    setOpen(true);
  }

  function set<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  function submit() {
    setError(null);
    const body = {
      name: form.name,
      kind: form.kind,
      address: form.address || null,
      contactName: form.contactName || null,
      contactPhone: form.contactPhone || null,
      contactEmail: form.contactEmail || null,
      note: form.note || null,
    };
    startTransition(async () => {
      const res = await saveLocation(form.id ?? null, body);
      if (!res.ok) {
        setError(res.error ?? "Uložení selhalo");
        return;
      }
      setOpen(false);
      router.refresh();
    });
  }

  async function remove(l: Location) {
    if (!(await confirm({ message: `Opravdu smazat místo „${l.name}"?`, danger: true, confirmLabel: "Smazat" }))) return;
    startTransition(async () => {
      const res = await deleteLocation(l.id);
      if (!res.ok) toast.error(res.error ?? "Smazání selhalo (místo může být použito programem).");
      router.refresh();
    });
  }

  return (
    <div>
      <div className="mb-4 flex justify-end">
        <Button onClick={openCreate}>
          <Plus className="h-4 w-4" /> Přidat místo
        </Button>
      </div>

      {!openCreateOnly && (
        <div className="rounded-lg border border-[var(--border)] bg-[var(--background)]">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Název</TableHead>
                <TableHead>Typ</TableHead>
                <TableHead>Adresa</TableHead>
                <TableHead></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {locations.map((l) => (
                <TableRow key={l.id}>
                  <TableCell className="font-medium">{l.name}</TableCell>
                  <TableCell>
                    <Badge>{KIND_LABELS[l.kind] ?? l.kind}</Badge>
                  </TableCell>
                  <TableCell className="text-sm text-[var(--muted-foreground)]">
                    {l.address || "—"}
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-1">
                      <IconAction label="Upravit" icon={Pencil} onClick={() => openEdit(l)} />
                      <IconAction
                        label="Smazat"
                        icon={Trash2}
                        onClick={() => remove(l)}
                        className="text-[var(--destructive)]"
                      />
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{form.id ? "Upravit místo" : "Nové místo"}</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4">
            <div className="flex flex-col gap-1.5">
              <Label>Název</Label>
              <Input value={form.name ?? ""} onChange={(e) => set("name", e.target.value)} />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label>Typ místa</Label>
              <Select
                className=""
                value={form.kind ?? "kindergarten"}
                onChange={(e) => set("kind", e.target.value as Location["kind"])}
              >
                <option value="kindergarten">Školka</option>
                <option value="school">Škola</option>
                <option value="venue">Sportoviště</option>
              </Select>
            </div>
            <div className="flex flex-col gap-1.5">
              <Label>Adresa</Label>
              <Input value={form.address ?? ""} onChange={(e) => set("address", e.target.value)} />
            </div>

            <div className="rounded-md border border-[var(--border)] p-3">
              <p className="mb-2 text-sm font-medium">Kontaktní osoba</p>
              <div className="grid gap-3">
                <Input
                  placeholder="Jméno (např. paní ředitelka)"
                  value={form.contactName ?? ""}
                  onChange={(e) => set("contactName", e.target.value)}
                />
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                  <Input
                    placeholder="Telefon"
                    value={form.contactPhone ?? ""}
                    onChange={(e) => set("contactPhone", e.target.value)}
                  />
                  <Input
                    placeholder="E-mail"
                    value={form.contactEmail ?? ""}
                    onChange={(e) => set("contactEmail", e.target.value)}
                  />
                </div>
              </div>
            </div>

            <div className="flex flex-col gap-1.5">
              <Label>Poznámka</Label>
              <Input value={form.note ?? ""} onChange={(e) => set("note", e.target.value)} />
            </div>

            {error && <p className="text-sm text-[var(--destructive)]">{error}</p>}
            <div className="flex justify-end gap-2">
              <Button variant="outline" onClick={() => setOpen(false)} disabled={isPending}>
                Zrušit
              </Button>
              <Button onClick={submit} disabled={isPending || !form.name}>
                {isPending ? "Ukládám…" : "Uložit"}
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
