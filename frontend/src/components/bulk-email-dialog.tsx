"use client";

import { useState, useTransition, useEffect } from "react";
import { Mail } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { fetchBulkRecipients, sendBulkEmail } from "@/lib/actions";

/** Dialog to email all parents of a program's active participants (prd §6A.3). */
export function BulkEmailDialog({ programId, programName }: { programId: string; programName: string }) {
  const [open, setOpen] = useState(false);
  const [subject, setSubject] = useState("");
  const [body, setBody] = useState("");
  const [recipients, setRecipients] = useState<string[] | null>(null);
  const [result, setResult] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isPending, start] = useTransition();

  useEffect(() => {
    if (open && recipients === null) {
      fetchBulkRecipients(programId).then((r) => setRecipients(r.emails));
    }
    if (!open) {
      setResult(null);
      setError(null);
    }
  }, [open, programId, recipients]);

  function send() {
    setError(null);
    setResult(null);
    start(async () => {
      const res = await sendBulkEmail(programId, subject, body);
      if (!res.ok) {
        setError(res.error ?? "Odeslání selhalo");
        return;
      }
      setResult(`Odesláno ${res.result?.sent}/${res.result?.total} příjemcům${res.result?.failed ? ` (${res.result.failed} selhalo)` : ""}.`);
      setSubject("");
      setBody("");
    });
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="outline" size="sm" title="Hromadný e-mail rodičům">
          <Mail className="h-3.5 w-3.5" /> E-mail rodičům
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Hromadný e-mail — {programName}</DialogTitle>
        </DialogHeader>
        <div className="grid gap-4">
          <p className="text-sm text-[var(--muted-foreground)]">
            {recipients === null
              ? "Načítám příjemce…"
              : `Odejde ${recipients.length} rodičům (aktivní registrace).`}
          </p>
          <div className="flex flex-col gap-1.5">
            <Label>Předmět</Label>
            <Input value={subject} onChange={(e) => setSubject(e.target.value)} placeholder="Info před startem kroužku" />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label>Text</Label>
            <textarea
              className="min-h-32 rounded-md border border-[var(--border)] bg-transparent px-3 py-2 text-sm"
              value={body}
              onChange={(e) => setBody(e.target.value)}
              placeholder="Dobrý den, ..."
            />
          </div>

          {error && <p className="text-sm text-[var(--destructive)]">{error}</p>}
          {result && <p className="text-sm text-success">{result}</p>}

          <div className="flex justify-end gap-2">
            <Button variant="outline" onClick={() => setOpen(false)} disabled={isPending}>
              Zavřít
            </Button>
            <Button
              onClick={send}
              disabled={isPending || !subject.trim() || !body.trim() || (recipients?.length ?? 0) === 0}
            >
              {isPending ? "Odesílám…" : "Odeslat"}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
