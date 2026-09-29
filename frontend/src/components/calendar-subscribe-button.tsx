"use client";

import { useState } from "react";
import { CalendarPlus, Copy, Check, ExternalLink } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";

/** Lets a user subscribe to their personal schedule as a live calendar feed (webcal/iCal). */
export function CalendarSubscribeButton() {
  const [open, setOpen] = useState(false);
  const [url, setUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [copied, setCopied] = useState(false);
  const [onlyMine, setOnlyMine] = useState(true);

  async function openDialog() {
    setOpen(true);
    if (url) return;
    setLoading(true);
    try {
      const res = await fetch("/api/calendar/token", { method: "POST" });
      const data = await res.json();
      setUrl(data.url as string);
    } catch {
      setUrl(null);
    } finally {
      setLoading(false);
    }
  }

  // Append the scope so trainers can subscribe to only their own approved shifts.
  const scopedUrl = url ? `${url}${onlyMine ? "?scope=mine" : ""}` : "";
  const webcalUrl = scopedUrl ? scopedUrl.replace(/^https?:\/\//, "webcal://") : "";

  async function copy() {
    if (!scopedUrl) return;
    await navigator.clipboard.writeText(scopedUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  return (
    <>
      <Button variant="outline" size="sm" onClick={openDialog}>
        <CalendarPlus className="h-4 w-4" /> Do kalendáře
      </Button>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Přidat rozvrh do kalendáře</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 text-sm">
            <p className="text-[var(--muted-foreground)]">
              Přihlas si svůj rozvrh do telefonu nebo počítače. Lekce se budou samy
              aktualizovat. Odkaz je osobní — nesdílej ho.
            </p>

            {loading ? (
              <p className="text-[var(--muted-foreground)]">Připravuji odkaz…</p>
            ) : url ? (
              <>
                <div className="flex flex-col gap-2 rounded-md border border-[var(--border)] p-3">
                  <label className="flex items-center gap-2">
                    <input
                      type="radio"
                      name="cal-scope"
                      checked={onlyMine}
                      onChange={() => setOnlyMine(true)}
                    />
                    <span>
                      <span className="font-medium text-[var(--foreground)]">Jen moje směny</span>
                      <span className="block text-xs text-[var(--muted-foreground)]">
                        Do kalendáře se přidají pouze směny, na které jsi schválený/á.
                      </span>
                    </span>
                  </label>
                  <label className="flex items-center gap-2">
                    <input
                      type="radio"
                      name="cal-scope"
                      checked={!onlyMine}
                      onChange={() => setOnlyMine(false)}
                    />
                    <span>
                      <span className="font-medium text-[var(--foreground)]">Celý rozvrh</span>
                      <span className="block text-xs text-[var(--muted-foreground)]">
                        Všechny lekce programů, ke kterým máš přístup.
                      </span>
                    </span>
                  </label>
                </div>

                <a href={webcalUrl}>
                  <Button className="w-full">
                    <ExternalLink className="h-4 w-4" /> Otevřít v kalendáři
                  </Button>
                </a>

                <div className="flex items-center gap-2">
                  <input
                    readOnly
                    value={scopedUrl}
                    onFocus={(e) => e.currentTarget.select()}
                    className="w-full truncate rounded-md border border-[var(--border)] bg-[var(--muted)]/30 px-3 py-2 text-xs"
                  />
                  <Button variant="outline" size="sm" onClick={copy} className="shrink-0">
                    {copied ? <Check className="h-4 w-4 text-success" /> : <Copy className="h-4 w-4" />}
                  </Button>
                </div>

                <div className="rounded-md border border-[var(--border)] p-3 text-xs text-[var(--muted-foreground)]">
                  <p className="mb-1 font-medium text-[var(--foreground)]">Ruční přidání:</p>
                  <p>• iPhone: Nastavení → Kalendář → Účty → Přidat účet → Jiný → Přidat odběr kalendáře → vlož odkaz</p>
                  <p>• Google Kalendář: Nastavení → Přidat kalendář → Z adresy URL → vlož odkaz</p>
                </div>
              </>
            ) : (
              <p className="text-[var(--destructive)]">Odkaz se nepodařilo načíst. Zkus to prosím znovu.</p>
            )}
          </div>
        </DialogContent>
      </Dialog>
    </>
  );
}
