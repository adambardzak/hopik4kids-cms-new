"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { Plus } from "lucide-react";
import type { Role, User } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Select } from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { changeUserRole, deactivateUser, inviteUser } from "@/lib/actions";

const ROLE_OPTIONS: { value: Role; label: string }[] = [
  { value: "owner", label: "Vlastník" },
  { value: "admin", label: "Správce" },
  { value: "trainer", label: "Trenér" },
  { value: "accountant", label: "Účetní" },
  { value: "viewer", label: "Náhled" },
];
const STATUS: Record<string, { label: string; variant: "success" | "warning" | "danger" }> = {
  active: { label: "Aktivní", variant: "success" },
  invited: { label: "Pozván", variant: "warning" },
  disabled: { label: "Deaktivován", variant: "danger" },
};

export function TeamManager({ users, currentUserId }: { users: User[]; currentUserId: string }) {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [email, setEmail] = useState("");
  const [role, setRole] = useState<Role>("trainer");
  const [error, setError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  function invite() {
    setError(null);
    startTransition(async () => {
      const res = await inviteUser(email, role);
      if (!res.ok) {
        setError(res.error ?? "Pozvánka selhala");
        return;
      }
      setOpen(false);
      setEmail("");
      router.refresh();
    });
  }

  function onRoleChange(u: User, newRole: string) {
    startTransition(async () => {
      const res = await changeUserRole(u.id, newRole);
      if (!res.ok) alert(res.error ?? "Změna role selhala");
      router.refresh();
    });
  }

  function onDeactivate(u: User) {
    if (!confirm(`Opravdu deaktivovat člena ${u.name}?`)) return;
    startTransition(async () => {
      const res = await deactivateUser(u.id);
      if (!res.ok) alert(res.error ?? "Deaktivace selhala");
      router.refresh();
    });
  }

  return (
    <div>
      <div className="mb-4 flex justify-end">
        <Button onClick={() => setOpen(true)}>
          <Plus className="h-4 w-4" /> Pozvat člena
        </Button>
      </div>

      <div className="rounded-lg border border-[var(--border)] bg-[var(--background)]">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Jméno</TableHead>
              <TableHead>E-mail</TableHead>
              <TableHead>Role</TableHead>
              <TableHead>Stav</TableHead>
              <TableHead></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {users.map((u) => {
              const st = STATUS[u.status] ?? STATUS.active;
              const isSelf = u.id === currentUserId;
              return (
                <TableRow key={u.id}>
                  <TableCell className="font-medium">{u.name}</TableCell>
                  <TableCell>{u.email}</TableCell>
                  <TableCell>
                    <Select
                      className="h-9"
                      value={u.role}
                      disabled={isPending || u.status === "disabled"}
                      onChange={(e) => onRoleChange(u, e.target.value)}
                    >
                      {ROLE_OPTIONS.map((r) => (
                        <option key={r.value} value={r.value}>
                          {r.label}
                        </option>
                      ))}
                    </Select>
                  </TableCell>
                  <TableCell>
                    <Badge variant={st.variant}>{st.label}</Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    {u.status !== "disabled" && !isSelf && (
                      <Button variant="ghost" size="sm" onClick={() => onDeactivate(u)}>
                        Deaktivovat
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </div>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Pozvat člena týmu</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4">
            <div className="flex flex-col gap-1.5">
              <Label>E-mail</Label>
              <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label>Role</Label>
              <Select
                className=""
                value={role}
                onChange={(e) => setRole(e.target.value as Role)}
              >
                {ROLE_OPTIONS.map((r) => (
                  <option key={r.value} value={r.value}>
                    {r.label}
                  </option>
                ))}
              </Select>
            </div>
            {error && <p className="text-sm text-[var(--destructive)]">{error}</p>}
            <div className="flex justify-end gap-2">
              <Button variant="outline" onClick={() => setOpen(false)} disabled={isPending}>
                Zrušit
              </Button>
              <Button onClick={invite} disabled={isPending || !email}>
                {isPending ? "Odesílám…" : "Odeslat pozvánku"}
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
