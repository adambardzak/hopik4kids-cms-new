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

type State = "unsupported" | "loading" | "default" | "granted" | "denied";

/** Button to enable PWA push notifications for the current admin/owner device. */
export function NotificationToggle() {
  const [state, setState] = useState<State>("loading");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (
      typeof window === "undefined" ||
      !("serviceWorker" in navigator) ||
      !("PushManager" in window) ||
      !("Notification" in window)
    ) {
      setState("unsupported");
      return;
    }
    setState(Notification.permission as State);
  }, []);

  async function enable() {
    setBusy(true);
    try {
      const permission = await Notification.requestPermission();
      if (permission !== "granted") {
        setState(permission as State);
        return;
      }
      // Get the VAPID public key from the backend.
      const keyRes = await fetch("/api/push/public-key");
      const { publicKey } = await keyRes.json();
      if (!publicKey) {
        alert("Notifikace nejsou na serveru nakonfigurované.");
        return;
      }
      const reg = await navigator.serviceWorker.ready;
      const sub = await reg.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(publicKey) as BufferSource,
      });
      const json = sub.toJSON();
      await fetch("/api/push/subscribe", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          endpoint: sub.endpoint,
          p256dh: json.keys?.p256dh,
          auth: json.keys?.auth,
        }),
      });
      setState("granted");
    } catch (e) {
      console.error("push subscribe failed", e);
      alert("Zapnutí notifikací selhalo. Zkus to prosím znovu.");
    } finally {
      setBusy(false);
    }
  }

  if (state === "unsupported" || state === "loading") return null;

  if (state === "granted") {
    return (
      <p className="mb-2 flex items-center gap-1.5 text-xs text-[var(--muted-foreground)]">
        <BellRing className="h-3.5 w-3.5 text-success" /> Notifikace zapnuté
      </p>
    );
  }

  if (state === "denied") {
    return (
      <p className="mb-2 flex items-center gap-1.5 text-xs text-[var(--muted-foreground)]">
        <BellOff className="h-3.5 w-3.5" /> Notifikace blokované v prohlížeči
      </p>
    );
  }

  return (
    <button
      onClick={enable}
      disabled={busy}
      className="mb-2 flex w-full items-center justify-center gap-1.5 rounded-md border border-[var(--border)] px-3 py-2 text-xs font-medium hover:bg-[var(--muted)] disabled:opacity-50"
    >
      <Bell className="h-3.5 w-3.5" />
      {busy ? "Zapínám…" : "Zapnout notifikace"}
    </button>
  );
}
