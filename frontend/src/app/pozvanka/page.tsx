"use client";

import { Suspense, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function InvitationPage() {
  return (
    <Suspense>
      <InvitationForm />
    </Suspense>
  );
}

function InvitationForm() {
  const router = useRouter();
  const params = useSearchParams();
  const token = params.get("token") ?? "";
  const [name, setName] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);
  const [loading, setLoading] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const res = await fetch("/api/auth/accept-invite", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ token, name, password }),
      });
      if (!res.ok) {
        const data = await res.json().catch(() => null);
        setError(data?.error ?? "Nepodařilo se přijmout pozvánku");
        return;
      }
      setDone(true);
      setTimeout(() => router.replace("/login"), 1500);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-[var(--muted)] p-4">
      <Card className="w-full max-w-sm">
        <CardHeader>
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src="/logo.svg" alt="Hopík4Kids" className="mb-2 h-12 w-auto" />
          <CardTitle>Dokončení registrace</CardTitle>
        </CardHeader>
        <CardContent>
          {!token ? (
            <p className="text-sm text-[var(--destructive)]">Chybí token pozvánky.</p>
          ) : done ? (
            <p className="text-sm">Účet aktivován. Přesměrování na přihlášení…</p>
          ) : (
            <form onSubmit={submit} className="flex flex-col gap-4">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="name">Jméno a příjmení</Label>
                <Input id="name" value={name} onChange={(e) => setName(e.target.value)} required />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="password">Heslo (min. 8 znaků)</Label>
                <Input
                  id="password"
                  type="password"
                  minLength={8}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              </div>
              {error && <p className="text-sm text-[var(--destructive)]">{error}</p>}
              <Button type="submit" size="lg" disabled={loading}>
                {loading ? "Aktivuji…" : "Aktivovat účet"}
              </Button>
            </form>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
