"use client";

import { useState, useTransition, useEffect } from "react";
import { useRouter } from "next/navigation";
import { Clock, Trash2 } from "lucide-react";
import type { WaitlistEntry } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { fetchWaitlist, setWaitlistStatus, deleteWaitlistEntry } from "@/lib/actions";
import { useConfirm } from "@/components/ui/confirm";

const STATUS: Record<string, { label: string; variant: "warning" | "info" | "success" | "danger" }> = {
  waiting: { label: "Čeká", variant: "warning" },
  offered: { label: "Nabídnuto", variant: "info" },
  converted: { label: "Přijato", variant: "success" },
  cancelled: { label: "Zrušeno", variant: "danger" },
};

/** Waitlist for a full program (prd §6A.2). */
export function WaitlistDialog({ programId, programName, triggerClassName }: { programId: string; programName: string; triggerClassName?: string }) {
  const router = useRouter();
  const confirm = useConfirm();
  const [open, setOpen] = useState(false);
  const [entries, setEntries] = useState<WaitlistEntry[] | null>(null);
  const [isPending, start] = useTransition();

  function reload() {
    fetchWaitlist(programId).then((r) => setEntries(r.items));
  }

  useEffect(() => {
    if (open) reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, programId]);

  const waiting = (entries ?? []).filter((e) => e.status === "waiting").length;

  function changeStatus(id: string, status: string) {
    start(async () => {
      await setWaitlistStatus(id, status);
      reload();
      router.refresh();
    });
  }

  function remove(id: string) {
    void (async () => {
      if (!(await confirm({ message: "Opravdu smazat záznam z čekací listiny?", danger: true, confirmLabel: "Smazat" }))) return;
      start(async () => {
        await deleteWaitlistEntry(id);
        reload();
        router.refresh();
      });
    })();
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="outline" size="sm" className={triggerClassName} title="Čekací listina">
          <Clock className="h-4 w-4" /> Čekatelé
        </Button>
      </DialogTrigger>
      <DialogContent className="max-h-[85vh] overflow-auto">
        <DialogHeader>
          <DialogTitle>Čekací listina — {programName}</DialogTitle>
        </DialogHeader>

        {entries === null ? (
          <p className="text-sm text-[var(--muted-foreground)]">Načítám…</p>
        ) : entries.length === 0 ? (
          <p className="text-sm text-[var(--muted-foreground)]">
            Zatím nikdo nečeká. Rodiče se přihlásí na čekací listinu z webu, když je program plný.
          </p>
        ) : (
          <div className="space-y-2">
            <p className="text-sm text-[var(--muted-foreground)]">{waiting}× čeká na uvolnění místa</p>
            {entries.map((e) => {
              const st = STATUS[e.status] ?? STATUS.waiting;
              return (
                <div
                  key={e.id}
                  className="flex items-start justify-between gap-3 rounded-md border border-[var(--border)] p-3"
                >
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="font-medium">{e.childName}</span>
                      <Badge variant={st.variant}>{st.label}</Badge>
                    </div>
                    <div className="mt-0.5 text-sm text-[var(--muted-foreground)]">
                      {e.parentName} · {e.parentPhone} ·{" "}
                      <a href={`mailto:${e.parentEmail}`} className="hover:underline">
                        {e.parentEmail}
                      </a>
                    </div>
                    {e.note && <div className="mt-1 text-sm">{e.note}</div>}
                    <div className="mt-0.5 text-xs text-[var(--muted-foreground)]">
                      Přihlášen: {new Date(e.createdAt).toLocaleDateString("cs-CZ")}
                    </div>
                  </div>
                  <div className="flex shrink-0 flex-col gap-1">
                    {e.status === "waiting" && (
                      <Button size="sm" variant="outline" disabled={isPending} onClick={() => changeStatus(e.id, "offered")}>
                        Nabídnout
                      </Button>
                    )}
                    {(e.status === "waiting" || e.status === "offered") && (
                      <Button size="sm" disabled={isPending} onClick={() => changeStatus(e.id, "converted")}>
                        Přijato
                      </Button>
                    )}
                    <Button
                      size="icon"
                      variant="ghost"
                      className="h-8 w-8 text-[var(--destructive)]"
                      disabled={isPending}
                      onClick={() => remove(e.id)}
                      title="Smazat"
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
