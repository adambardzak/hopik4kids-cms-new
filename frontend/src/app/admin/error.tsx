"use client";

import { useEffect } from "react";
import Link from "next/link";
import { AlertTriangle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";

/**
 * Admin error boundary — shows a Czech message instead of Next's generic English error page.
 * Detects auth errors (message contains 401/403 or auth codes) and offers re-login.
 */
export default function AdminError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  const msg = error?.message ?? "";
  const isAuth =
    /401|403|INVALID_CREDENTIALS|Unauthorized|Forbidden|Nepovolený|Neplatné přihlašovací/i.test(msg);

  return (
    <div className="flex min-h-[60vh] items-center justify-center p-4">
      <Card className="w-full max-w-md">
        <CardContent className="flex flex-col items-center gap-4 p-8 text-center">
          <div className="flex h-12 w-12 items-center justify-center rounded-full" style={{ background: "var(--warning-bg)" }}>
            <AlertTriangle className="h-6 w-6 text-warning" />
          </div>
          {isAuth ? (
            <>
              <div>
                <h2 className="text-lg font-semibold">Přihlášení vypršelo</h2>
                <p className="mt-1 text-sm text-[var(--muted-foreground)]">
                  Z bezpečnostních důvodů se přihlas znovu.
                </p>
              </div>
              <Button asChild size="lg">
                <Link href="/login">Přihlásit se</Link>
              </Button>
            </>
          ) : (
            <>
              <div>
                <h2 className="text-lg font-semibold">Něco se pokazilo</h2>
                <p className="mt-1 text-sm text-[var(--muted-foreground)]">
                  Nepodařilo se načíst stránku. Zkus to prosím znovu.
                </p>
              </div>
              <div className="flex gap-2">
                <Button onClick={reset} size="lg">
                  Zkusit znovu
                </Button>
                <Button asChild variant="outline" size="lg">
                  <Link href="/admin">Přehled</Link>
                </Button>
              </div>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
