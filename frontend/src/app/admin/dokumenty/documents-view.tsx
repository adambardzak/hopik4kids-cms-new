"use client";

import { useRouter } from "next/navigation";
import { useMemo, useState, useTransition } from "react";
import { Plus, Search, FileText, ExternalLink, Pencil, Trash2, ChevronDown, ChevronRight } from "lucide-react";
import type { DocumentCategory, DocumentItem } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Select } from "@/components/ui/select";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { saveDocument, deleteDocument } from "@/lib/actions";

const CATEGORIES: { value: DocumentCategory; label: string }[] = [
  { value: "pravidla", label: "Pravidla" },
  { value: "metodika", label: "Metodika" },
  { value: "checklist", label: "Checklisty" },
  { value: "formular", label: "Formuláře" },
  { value: "ostatni", label: "Ostatní" },
];
const CAT_LABEL = Object.fromEntries(CATEGORIES.map((c) => [c.value, c.label]));

type FormState = Partial<DocumentItem> & { fileId?: string | null };

export function DocumentsView({ documents, canEdit }: { documents: DocumentItem[]; canEdit: boolean }) {
  const router = useRouter();
  const [q, setQ] = useState("");
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<FormState>({});
  const [error, setError] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [expanded, setExpanded] = useState<string | null>(null);
  const [isPending, start] = useTransition();

  const filtered = useMemo(() => {
    const needle = q.trim().toLowerCase();
    if (!needle) return documents;
    return documents.filter((d) => d.title.toLowerCase().includes(needle));
  }, [documents, q]);

  const grouped = useMemo(() => {
    const map = new Map<string, DocumentItem[]>();
    for (const d of filtered) {
      (map.get(d.category) ?? map.set(d.category, []).get(d.category)!).push(d);
    }
    return CATEGORIES.map((c) => ({ ...c, docs: map.get(c.value) ?? [] })).filter((g) => g.docs.length > 0);
  }, [filtered]);

  function openCreate() {
    setForm({ category: "pravidla", visibility: "trainers" });
    setError(null);
    setOpen(true);
  }
  function openEdit(d: DocumentItem) {
    setForm({ ...d, fileId: undefined });
    setError(null);
    setOpen(true);
  }

  async function uploadFile(file: File) {
    setUploading(true);
    setError(null);
    try {
      const fd = new FormData();
      fd.append("file", file);
      fd.append("alt", file.name);
      const res = await fetch("/api/media", { method: "POST", body: fd });
      const data = await res.json();
      if (!res.ok) {
        setError(data?.error ?? "Nahrání souboru selhalo");
        return;
      }
      setForm((f) => ({ ...f, fileId: data.id, fileUrl: data.url, fileName: file.name }));
    } finally {
      setUploading(false);
    }
  }

  function submit() {
    setError(null);
    const body = {
      title: form.title,
      category: form.category,
      visibility: form.visibility,
      content: form.content || null,
      // send fileId only if a new file was uploaded; keep existing otherwise via fileUrl trick
      fileId: form.fileId !== undefined ? form.fileId : undefined,
      sortOrder: form.sortOrder ?? 0,
    };
    start(async () => {
      const res = await saveDocument(form.id ?? null, body);
      if (!res.ok) {
        setError(res.error ?? "Uložení selhalo");
        return;
      }
      setOpen(false);
      router.refresh();
    });
  }

  function remove(d: DocumentItem) {
    if (!confirm(`Opravdu smazat „${d.title}"?`)) return;
    start(async () => {
      const res = await deleteDocument(d.id);
      if (!res.ok) alert(res.error ?? "Smazání selhalo");
      router.refresh();
    });
  }

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <div className="relative">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--muted-foreground)]" />
          <Input className="w-full pl-9 sm:w-64" placeholder="Hledat dokument…" value={q} onChange={(e) => setQ(e.target.value)} />
        </div>
        {canEdit && (
          <Button className="ml-auto" onClick={openCreate}>
            <Plus className="h-4 w-4" /> Přidat dokument
          </Button>
        )}
      </div>

      {grouped.length === 0 ? (
        <div className="rounded-lg border border-dashed border-[var(--border)] bg-[var(--background)] p-12 text-center text-sm text-[var(--muted-foreground)]">
          {documents.length === 0 ? "Zatím žádné dokumenty." : "Nic nenalezeno."}
        </div>
      ) : (
        <div className="space-y-6">
          {grouped.map((g) => (
            <div key={g.value}>
              <h3 className="mb-2 text-sm font-semibold uppercase tracking-wide text-[var(--muted-foreground)]">
                {g.label}
              </h3>
              <div className="space-y-2">
                {g.docs.map((d) => (
                  <div key={d.id} className="rounded-lg border border-[var(--border)] bg-[var(--background)]">
                    <div className="flex items-center gap-3 p-3">
                      <FileText className="h-4 w-4 shrink-0 text-[var(--muted-foreground)]" />
                      <button
                        className="flex-1 text-left font-medium"
                        onClick={() => (d.content ? setExpanded(expanded === d.id ? null : d.id) : undefined)}
                      >
                        {d.title}
                      </button>
                      {d.visibility === "admin" && <Badge variant="default">Jen admin</Badge>}
                      {d.fileUrl && (
                        <a
                          href={d.fileUrl}
                          target="_blank"
                          rel="noreferrer"
                          className="flex items-center gap-1 text-sm text-[var(--primary)] hover:underline"
                        >
                          <ExternalLink className="h-3.5 w-3.5" /> Otevřít
                        </a>
                      )}
                      {d.content && (
                        <button onClick={() => setExpanded(expanded === d.id ? null : d.id)}>
                          {expanded === d.id ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
                        </button>
                      )}
                      {canEdit && (
                        <>
                          <button onClick={() => openEdit(d)} className="text-[var(--muted-foreground)] hover:text-[var(--foreground)]" title="Upravit">
                            <Pencil className="h-4 w-4" />
                          </button>
                          <button onClick={() => remove(d)} className="text-[var(--destructive)]" title="Smazat">
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </>
                      )}
                    </div>
                    {d.content && expanded === d.id && (
                      <div
                        className="border-t border-[var(--border)] px-4 py-3 text-sm whitespace-pre-wrap"
                        dangerouslySetInnerHTML={{ __html: d.content }}
                      />
                    )}
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Editor (admin) */}
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-h-[85vh] overflow-auto">
          <DialogHeader>
            <DialogTitle>{form.id ? "Upravit dokument" : "Nový dokument"}</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4">
            <div className="flex flex-col gap-1.5">
              <Label>Název</Label>
              <Input value={form.title ?? ""} onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))} />
            </div>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div className="flex flex-col gap-1.5">
                <Label>Kategorie</Label>
                <Select
                  value={form.category ?? "pravidla"}
                  onChange={(e) => setForm((f) => ({ ...f, category: e.target.value as DocumentCategory }))}
                >
                  {CATEGORIES.map((c) => (
                    <option key={c.value} value={c.value}>
                      {c.label}
                    </option>
                  ))}
                </Select>
              </div>
              <div className="flex flex-col gap-1.5">
                <Label>Viditelnost</Label>
                <Select
                  value={form.visibility ?? "trainers"}
                  onChange={(e) => setForm((f) => ({ ...f, visibility: e.target.value as "trainers" | "admin" }))}
                >
                  <option value="trainers">Trenéři</option>
                  <option value="admin">Jen admin</option>
                </Select>
              </div>
            </div>
            <div className="flex flex-col gap-1.5">
              <Label>Soubor (PDF/obrázek)</Label>
              {(form.fileUrl || form.fileName) && (
                <p className="text-xs text-[var(--muted-foreground)]">Aktuální: {form.fileName ?? form.fileUrl}</p>
              )}
              <input type="file" accept="application/pdf,image/*" onChange={(e) => e.target.files?.[0] && uploadFile(e.target.files[0])} className="text-sm" />
              {uploading && <p className="text-xs text-[var(--muted-foreground)]">Nahrávám…</p>}
            </div>
            <div className="flex flex-col gap-1.5">
              <Label>Text dokumentu (nepovinné, HTML)</Label>
              <textarea
                className="min-h-32 rounded-md border border-[var(--border)] bg-transparent px-3 py-2 text-sm"
                value={form.content ?? ""}
                onChange={(e) => setForm((f) => ({ ...f, content: e.target.value }))}
                placeholder="Napiš pravidla / metodiku přímo sem, nebo nahraj soubor výše."
              />
            </div>

            {error && <p className="text-sm text-[var(--destructive)]">{error}</p>}
            <div className="flex justify-end gap-2">
              <Button variant="outline" onClick={() => setOpen(false)} disabled={isPending}>
                Zrušit
              </Button>
              <Button onClick={submit} disabled={isPending || !form.title}>
                {isPending ? "Ukládám…" : "Uložit"}
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
