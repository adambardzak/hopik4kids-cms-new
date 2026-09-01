"use client";

import { useEffect, useState } from "react";
import { Bell, BellOff, BellRing } from "lucide-react";

/** base64url VAPID key -> Uint8Array for PushManager.subscribe. */
function urlBase64ToUint8Array(base64String: string): Uint8Array {
  const padding = "=".repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, "+").replace(/_/g, "/");
  const raw = atob(base64);
  const arr = new Uint8Array(raw.length);
  for (let i = 0; i < raw.length; i++) arr[i] = raw.charCodeAt(i);
  return arr;
}

type State = "unsupported" | "loading" | "prompt" | "subscribed" | "denied";

/** Get an active service-worker registration reliably (iOS `serviceWorker.ready` can hang). */
async function getRegistration(): Promise<ServiceWorkerRegistration | null> {
  try {
    // Already registered?
    const existing = await navigator.serviceWorker.getRegistration();
    if (existing) {
      // Wait briefly for activation, but never hang.
      if (existing.active) return existing;
      await Promise.race([
        new Promise<void>((resolve) => {
          const sw = existing.installing || existing.waiting;
          if (!sw) return resolve();
          sw.addEventListener("statechange", () => sw.state === "activated" && resolve());
        }),
        new Promise<void>((resolve) => setTimeout(resolve, 4000)),
      ]);
      return existing;
    }
    // Not registered yet — register now (returns once registered).
    return await navigator.serviceWorker.register("/sw.js");
  } catch {
    return null;
  }
}

/**
 * Button to enable PWA push notifications for the current admin/owner device.
 * Verifies an actual server-registered subscription (not just Notification.permission),
 * so a silently-failed subscribe can be retried instead of falsely showing "enabled".
 */
export function NotificationToggle() {
  const [state, setState] = useState<State>("loading");
  const [busy, setBusy] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useEffect(() => {
    void refresh();
  }, []);

  async function refresh() {
    if (
      typeof window === "undefined" ||
      !("serviceWorker" in navigator) ||
      !("PushManager" in window) ||
      !("Notification" in window)
    ) {
      setState("unsupported");
      return;
    }
    if (Notification.permission === "denied") {
      setState("denied");
      return;
    }
    try {
      // Don't hang forever if the service worker never becomes ready (e.g. first launch).
      const reg = await getRegistration();
      if (!reg) {
        // SW not ready yet — still let the user try to enable (subscribe re-checks readiness).
        setState("prompt");
        return;
      }
      const sub = await reg.pushManager.getSubscription();
      // Only treat as "subscribed" when permission is granted AND a real subscription exists.
      setState(Notification.permission === "granted" && sub ? "subscribed" : "prompt");
    } catch {
      setState("prompt");
    }
  }

  async function enable() {
    setBusy(true);
    setErrorMsg(null);
    try {
      const permission = await Notification.requestPermission();
      if (permission === "denied") {
        setState("denied");
        return;
      }
      if (permission !== "granted") {
        setState("prompt");
        return;
      }

      const keyRes = await fetch("/api/push/public-key");
      if (!keyRes.ok) throw new Error("Nepodařilo se načíst klíč ze serveru.");
      const { publicKey } = await keyRes.json();
      if (!publicKey) throw new Error("Notifikace nejsou na serveru nakonfigurované.");

      const reg = await getRegistration();
      if (!reg) throw new Error("Aplikaci nejdřív přidej na plochu a otevři z ikony.");

      // Reuse an existing subscription if present; otherwise create one.
      let sub = await reg.pushManager.getSubscription();
      if (!sub) {
        sub = await reg.pushManager.subscribe({
          userVisibleOnly: true,
          applicationServerKey: urlBase64ToUint8Array(publicKey) as BufferSource,
        });
      }

      const json = sub.toJSON();
      const res = await fetch("/api/push/subscribe", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          endpoint: sub.endpoint,
          p256dh: json.keys?.p256dh,
          auth: json.keys?.auth,
        }),
      });
      if (!res.ok) {
        // Roll back the local subscription so the next attempt is clean.
        try {
          await sub.unsubscribe();
        } catch {
          /* ignore */
        }
        throw new Error("Server odmítl registraci (kód " + res.status + ").");
      }

      setState("subscribed");
    } catch (e) {
      console.error("push subscribe failed", e);
      setErrorMsg(e instanceof Error ? e.message : "Zapnutí notifikací selhalo.");
      setState("prompt");
    } finally {
      setBusy(false);
    }
  }

  if (state === "loading") return null;

  if (state === "unsupported") {
    return (
      <p className="mb-2 flex items-start gap-1.5 text-xs text-[var(--muted-foreground)]">
        <BellOff className="mt-0.5 h-3.5 w-3.5 shrink-0" />
        <span>
          Notifikace vyžadují aplikaci na ploše (Přidat na plochu) a iPhone iOS 16.4+.
        </span>
      </p>
    );
  }

  if (state === "subscribed") {
    return (
      <p className="mb-2 flex items-center gap-1.5 text-xs text-[var(--muted-foreground)]">
        <BellRing className="h-3.5 w-3.5 text-success" /> Notifikace zapnuté
      </p>
    );
  }

  if (state === "denied") {
    return (
      <p className="mb-2 flex items-start gap-1.5 text-xs text-[var(--muted-foreground)]">
        <BellOff className="mt-0.5 h-3.5 w-3.5 shrink-0" />
        <span>Notifikace jsou blokované. Povol je v nastavení telefonu → aplikace → Hopík4Kids → Oznámení.</span>
      </p>
    );
  }

  return (
    <div className="mb-2">
      <button
        onClick={enable}
        disabled={busy}
        className="flex w-full items-center justify-center gap-1.5 rounded-md border border-[var(--border)] px-3 py-2 text-xs font-medium hover:bg-[var(--muted)] disabled:opacity-50"
      >
        <Bell className="h-3.5 w-3.5" />
        {busy ? "Zapínám…" : "Zapnout notifikace"}
      </button>
      {errorMsg && <p className="mt-1 text-xs text-[var(--destructive)]">{errorMsg}</p>}
    </div>
  );
}
