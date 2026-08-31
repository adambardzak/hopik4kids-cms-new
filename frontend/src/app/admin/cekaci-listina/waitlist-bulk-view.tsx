"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { Users, Mail, Trash2, Check, X, Send, RefreshCw } from "lucide-react";
import type { Program, WaitlistEntry } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Select } from "@/components/ui/select";
import { Card, CardContent } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  fetchWaitlist,
  setWaitlistStatus,
  deleteWaitlistEntry,
  fetchBulkRecipients,
  sendBulkEmail,
} from "@/lib/actions";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm";

const WAITLIST_STATUS: Record<
  WaitlistEntry["status"],
  { label: string; variant: "success" | "warning" | "danger" | "default" }
> = {
  waiting: { label: "Čeká", variant: "warning" },
  offered: { label: "Nabídnuto", variant: "default" },
  converted: { label: "Zapsáno", variant: "success" },
  cancelled: { label: "Zrušeno", variant: "danger" },
};

export function WaitlistBulkView({ programs }: { programs: Program[] }) {
  const [tab, setTab] = useState<"waitlist" | "bulk">("waitlist");

  return (
    <div>
      <div className="mb-4 flex gap-2 border-b border-[var(--border)]">
        <TabButton active={tab === "waitlist"} onClick={() => setTab("waitlist")} icon={Users}>
          Čekací listina
        </TabButton>
        <TabButton active={tab === "bulk"} onClick={() => setTab("bulk")} icon={Mail}>
          Hromadný e-mail
        </TabButton>
      </div>

      {tab === "waitlist" ? (
        <WaitlistPanel programs={programs} />
      ) : (
        <BulkEmailPanel programs={programs} />
      )}
    </div>
  );
}

function TabButton({
  active,
  onClick,
  icon: Icon,
  children,
}: {
  active: boolean;
  onClick: () => void;
  icon: React.ComponentType<{ className?: string }>;
  children: React.ReactNode;
}) {
  return (
    <button
      onClick={onClick}
      className={`-mb-px flex items-center gap-1.5 border-b-2 px-4 py-2 text-sm font-medium ${
        active
          ? "border-[var(--primary)] text-[var(--primary)]"
          : "border-transparent text-[var(--muted-foreground)]"
      }`}
    >
      <Icon className="h-4 w-4" />
      {children}
    </button>
  );
}

function ProgramPicker({
  programs,
  value,
  onChange,
}: {
  programs: Program[];
  value: string;
  onChange: (id: string) => void;
}) {
  return (
    <div className="mb-4 max-w-md">
      <Label>Program</Label>
      <Select value={value} onChange={(e) => onChange(e.target.value)}>
        <option value="">— vyber program —</option>
        {programs.map((p) => (
          <option key={p.id} value={p.id}>
            {p.name}
          </option>
        ))}
      </Select>
    </div>
  );
}

function WaitlistPanel({ programs }: { programs: Program[] }) {
  const router = useRouter();
  const toast = useToast();
  const confirm = useConfirm();
  const [program, setProgram] = useState("");
  const [items, setItems] = useState<WaitlistEntry[]>([]);
  const [loaded, setLoaded] = useState(false);
  const [pending, start] = useTransition();

  function load(id: string) {
    setProgram(id);
    setLoaded(false);
    if (!id) {
      setItems([]);
      return;
    }
    start(async () => {
      const res = await fetchWaitlist(id);
      setItems(res.items);
      setLoaded(true);
    });
  }

  function changeStatus(entry: WaitlistEntry, status: string) {
    start(async () => {
      const res = await setWaitlistStatus(entry.id, status);
      if (res.ok) load(program);
      else toast.error(res.error ?? "Změna se nezdařila");
    });
  }

  async function remove(entry: WaitlistEntry) {
    if (!(await confirm({ message: `Odstranit ${entry.childName} z čekací listiny?`, danger: true, confirmLabel: "Odstranit" }))) return;
    start(async () => {
      const res = await deleteWaitlistEntry(entry.id);
      if (res.ok) load(program);
      else toast.error(res.error ?? "Smazání se nezdařilo");
    });
  }

  const waiting = items.filter((i) => i.status === "waiting").length;

  return (
    <div>
      <ProgramPicker programs={programs} value={program} onChange={load} />

      {!program ? (
        <p className="text-sm text-[var(--muted-foreground)]">
          Vyber program pro zobrazení zájemců na čekací listině.
        </p>
      ) : pending && !loaded ? (
        <p className="text-sm text-[var(--muted-foreground)]">Načítám…</p>
      ) : items.length === 0 ? (
        <Card>
          <CardContent className="p-6 text-sm text-[var(--muted-foreground)]">
            Na čekací listině tohoto programu nikdo není.
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="p-0">
            <div className="flex items-center justify-between border-b border-[var(--border)] p-3 text-sm">
              <span className="font-medium">
                {items.length} zájemců · {waiting} čeká
              </span>
              <Button variant="ghost" size="sm" onClick={() => load(program)} disabled={pending}>
                <RefreshCw className="h-4 w-4" /> Obnovit
              </Button>
            </div>
            <div className="overflow-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Dítě</TableHead>
                    <TableHead>Rodič</TableHead>
                    <TableHead>Stav</TableHead>
                    <TableHead className="text-right">Akce</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {items.map((e) => {
                    const st = WAITLIST_STATUS[e.status];
                    return (
                      <TableRow key={e.id}>
                        <TableCell className="font-medium">
                          {e.childName}
                          {e.note && (
                            <div className="text-xs font-normal text-[var(--muted-foreground)]">
                              {e.note}
                            </div>
                          )}
                        </TableCell>
                        <TableCell className="text-sm">
                          <div>{e.parentName}</div>
                          <div className="text-xs text-[var(--muted-foreground)]">
                            <a href={`tel:${e.parentPhone}`} className="hover:underline">
                              {e.parentPhone}
                            </a>{" "}
                            ·{" "}
                            <a href={`mailto:${e.parentEmail}`} className="hover:underline">
                              {e.parentEmail}
                            </a>
                          </div>
                        </TableCell>
                        <TableCell>
                          <Badge variant={st.variant}>{st.label}</Badge>
                        </TableCell>
                        <TableCell className="text-right">
                          <div className="flex justify-end gap-1">
                            {e.status !== "offered" && e.status !== "converted" && (
                              <Button
                                variant="ghost"
                                size="sm"
                                title="Označit jako nabídnuto"
                                onClick={() => changeStatus(e, "offered")}
                                disabled={pending}
                              >
                                <Mail className="h-4 w-4" />
                              </Button>
                            )}
                            {e.status !== "converted" && (
                              <Button
                                variant="ghost"
                                size="sm"
                                title="Označit jako zapsáno"
                                onClick={() => changeStatus(e, "converted")}
                                disabled={pending}
                              >
                                <Check className="h-4 w-4 text-success" />
                              </Button>
                            )}
                            {e.status !== "cancelled" && (
                              <Button
                                variant="ghost"
                                size="sm"
                                title="Zrušit"
                                onClick={() => changeStatus(e, "cancelled")}
                                disabled={pending}
                              >
                                <X className="h-4 w-4" />
                              </Button>
                            )}
                            <Button
                              variant="ghost"
                              size="sm"
                              title="Smazat"
                              onClick={() => remove(e)}
                              disabled={pending}
                            >
                              <Trash2 className="h-4 w-4 text-[var(--destructive)]" />
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}

function BulkEmailPanel({ programs }: { programs: Program[] }) {
  const toast = useToast();
  const confirm = useConfirm();
  const [program, setProgram] = useState("");
  const [recipients, setRecipients] = useState<string[]>([]);
  const [loadedFor, setLoadedFor] = useState("");
  const [subject, setSubject] = useState("");
  const [body, setBody] = useState("");
  const [pending, start] = useTransition();

  function pick(id: string) {
    setProgram(id);
    setRecipients([]);
    setLoadedFor("");
    if (!id) return;
    start(async () => {
      const res = await fetchBulkRecipients(id);
      setRecipients(res.emails);
      setLoadedFor(id);
    });
  }

  async function send() {
    if (!subject.trim() || !body.trim()) {
      toast.error("Vyplň předmět i text zprávy.");
      return;
    }
    if (!(await confirm({ message: `Odeslat e-mail ${recipients.length} příjemcům?`, confirmLabel: "Odeslat" }))) return;
    start(async () => {
      const res = await sendBulkEmail(program, subject, body);
      if (res.ok && res.result) {
        toast.success(
          `Odesláno: ${res.result.sent} z ${res.result.total}` +
            (res.result.failed > 0 ? ` (${res.result.failed} selhalo)` : ""),
        );
        setSubject("");
        setBody("");
      } else {
        toast.error(res.error ?? "Odeslání selhalo");
      }
    });
  }

  return (
    <div className="max-w-2xl">
      <ProgramPicker programs={programs} value={program} onChange={pick} />

      {!program ? (
        <p className="text-sm text-[var(--muted-foreground)]">
          Vyber program — e-mail se odešle rodičům jeho aktivních účastníků.
        </p>
      ) : (
        <Card>
          <CardContent className="space-y-4 p-5">
            <p className="text-sm text-[var(--muted-foreground)]">
              {loadedFor === program
                ? `${recipients.length} příjemců (rodičů aktivních dětí).`
                : "Načítám příjemce…"}
            </p>

            <div>
              <Label>Předmět</Label>
              <Input
                value={subject}
                onChange={(e) => setSubject(e.target.value)}
                placeholder="Např. Důležitá informace k tréninku"
              />
            </div>

            <div>
              <Label>Text zprávy</Label>
              <textarea
                value={body}
                onChange={(e) => setBody(e.target.value)}
                rows={8}
                placeholder="Dobrý den, …"
                className="w-full rounded-md border border-[var(--border)] bg-[var(--background)] px-3 py-2 text-sm"
              />
            </div>

            <Button onClick={send} disabled={pending || recipients.length === 0}>
              <Send className="h-4 w-4" />
              {pending ? "Odesílám…" : `Odeslat ${recipients.length} příjemcům`}
            </Button>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
