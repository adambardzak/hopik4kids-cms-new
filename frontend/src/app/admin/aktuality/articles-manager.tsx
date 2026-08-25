"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { Plus } from "lucide-react";
import type { Article, Media } from "@/lib/types";
import { Button } from "@/components/ui/button";
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
import { deleteArticle, saveArticle } from "@/lib/actions";

function slugify(s: string): string {
  return s
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

type FormState = Partial<Article> & { slugTouched?: boolean };

export function ArticlesManager({
  articles,
  openCreateOnly,
}: {
  articles: Article[];
  openCreateOnly?: boolean;
}) {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<FormState>({});
  const [error, setError] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [isPending, startTransition] = useTransition();

  function openCreate() {
    setForm({});
    setError(null);
    setOpen(true);
  }

  function openEdit(a: Article) {
    setForm({ ...a, slugTouched: true });
    setError(null);
    setOpen(true);
  }

  function setTitle(title: string) {
    setForm((f) => ({ ...f, title, slug: f.slugTouched ? f.slug : slugify(title) }));
  }

  async function uploadCover(file: File) {
    setUploading(true);
    setError(null);
    try {
      const fd = new FormData();
      fd.append("file", file);
      const res = await fetch("/api/media", { method: "POST", body: fd });
      const data = await res.json();
      if (!res.ok) {
        setError(data?.error ?? "Nahrání obrázku selhalo");
        return;
      }
      const media = data as Media;
      setForm((f) => ({ ...f, coverId: media.id, coverUrl: media.url }));
    } finally {
      setUploading(false);
    }
  }

  function submit(publish: boolean) {
    setError(null);
    const body = {
      title: form.title,
      slug: form.slug,
      excerpt: form.excerpt || null,
      content: form.content || null,
      coverId: form.coverId || null,
      publishedAt: publish
        ? form.publishedAt ?? new Date().toISOString()
        : null,
    };
    startTransition(async () => {
      const res = await saveArticle(form.id ?? null, body);
      if (!res.ok) {
        setError(res.error ?? "Uložení selhalo");
        return;
      }
      setOpen(false);
      router.refresh();
    });
  }

  function remove(a: Article) {
    if (!confirm(`Opravdu smazat článek „${a.title}"?`)) return;
    startTransition(async () => {
      await deleteArticle(a.id);
      router.refresh();
    });
  }

  return (
    <div>
      <div className="mb-4 flex justify-end">
        <Button onClick={openCreate}>
          <Plus className="h-4 w-4" /> Nová aktualita
        </Button>
      </div>

      {!openCreateOnly && (
        <div className="rounded-lg border border-[var(--border)] bg-[var(--background)]">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Titulek</TableHead>
                <TableHead>Stav</TableHead>
                <TableHead>Publikováno</TableHead>
                <TableHead></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {articles.map((a) => (
                <TableRow key={a.id}>
                  <TableCell className="font-medium">{a.title}</TableCell>
                  <TableCell>
                    <Badge variant={a.published ? "success" : "default"}>
                      {a.published ? "Publikováno" : "Koncept"}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-sm text-[var(--muted-foreground)]">
                    {a.publishedAt ? new Date(a.publishedAt).toLocaleDateString("cs-CZ") : "—"}
                  </TableCell>
                  <TableCell className="text-right">
                    <Button variant="ghost" size="sm" onClick={() => openEdit(a)}>
                      Upravit
                    </Button>
                    <Button variant="ghost" size="sm" onClick={() => remove(a)}>
                      Smazat
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-h-[85vh] overflow-auto">
          <DialogHeader>
            <DialogTitle>{form.id ? "Upravit aktualitu" : "Nová aktualita"}</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4">
            <div className="flex flex-col gap-1.5">
              <Label>Titulek</Label>
              <Input value={form.title ?? ""} onChange={(e) => setTitle(e.target.value)} />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label>Slug (URL)</Label>
              <Input
                value={form.slug ?? ""}
                onChange={(e) => setForm((f) => ({ ...f, slug: e.target.value, slugTouched: true }))}
              />
              {form.slug && (
                <p className="text-xs text-[var(--muted-foreground)]">hopik4kids.cz/aktuality/{form.slug}</p>
              )}
            </div>
            <div className="flex flex-col gap-1.5">
              <Label>Perex</Label>
              <textarea
                className="min-h-20 rounded-md border border-[var(--border)] bg-transparent px-3 py-2 text-sm"
                value={form.excerpt ?? ""}
                onChange={(e) => setForm((f) => ({ ...f, excerpt: e.target.value }))}
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label>Obsah (HTML)</Label>
              <textarea
                className="min-h-40 rounded-md border border-[var(--border)] bg-transparent px-3 py-2 font-mono text-xs"
                value={form.content ?? ""}
                onChange={(e) => setForm((f) => ({ ...f, content: e.target.value }))}
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label>Úvodní obrázek</Label>
              {form.coverUrl && (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={form.coverUrl} alt="" className="mb-2 h-32 w-auto rounded-md object-cover" />
              )}
              <input
                type="file"
                accept="image/*"
                onChange={(e) => e.target.files?.[0] && uploadCover(e.target.files[0])}
                className="text-sm"
              />
              {uploading && <p className="text-xs text-[var(--muted-foreground)]">Nahrávám…</p>}
            </div>

            {error && <p className="text-sm text-[var(--destructive)]">{error}</p>}
            <div className="flex justify-end gap-2">
              <Button variant="outline" onClick={() => submit(false)} disabled={isPending}>
                Uložit koncept
              </Button>
              <Button onClick={() => submit(true)} disabled={isPending}>
                {isPending ? "Ukládám…" : "Publikovat"}
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
