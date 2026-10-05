"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Mail, Paperclip } from "lucide-react";
import type { EmailLogEntry } from "@/lib/admin-data";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { EmptyState } from "@/components/page-header";

function fmt(iso: string) {
  return new Date(iso).toLocaleString("cs-CZ", { dateStyle: "short", timeStyle: "short" });
}

export function EmailLogView({ items, q }: { items: EmailLogEntry[]; q: string }) {
  const router = useRouter();
  const [search, setSearch] = useState(q);

  return (
    <div className="flex flex-col gap-4">
      <form
        className="flex gap-2"
        onSubmit={(e) => {
          e.preventDefault();
          router.push(search.trim() ? `/admin/emaily?q=${encodeURIComponent(search.trim())}` : "/admin/emaily");
        }}
      >
        <Input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Hledat podle e-mailu příjemce nebo předmětu…"
          className="max-w-md"
        />
        <Button type="submit">Hledat</Button>
      </form>

      {items.length === 0 ? (
        <EmptyState icon={Mail} message="Žádné odeslané e-maily. Záznamy se ukládají od nasazení této funkce." />
      ) : (
        <div className="overflow-auto rounded-lg border border-[var(--border)] bg-[var(--background)]">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Kdy</TableHead>
                <TableHead>Komu</TableHead>
                <TableHead>Předmět</TableHead>
                <TableHead>Stav</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {items.map((e) => (
                <TableRow key={e.id}>
                  <TableCell className="whitespace-nowrap text-sm">{fmt(e.sentAt)}</TableCell>
                  <TableCell className="text-sm">{e.recipient}</TableCell>
                  <TableCell className="text-sm">
                    {e.subject}
                    {e.attachment && <Paperclip className="ml-1 inline h-3 w-3 text-[var(--muted-foreground)]" />}
                  </TableCell>
                  <TableCell>
                    {e.success ? (
                      <Badge variant="success">Odesláno</Badge>
                    ) : (
                      <div className="flex flex-col gap-1">
                        <Badge variant="danger">Chyba</Badge>
                        {e.error && <span className="max-w-xs text-xs text-[var(--muted-foreground)]">{e.error}</span>}
                      </div>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  );
}
