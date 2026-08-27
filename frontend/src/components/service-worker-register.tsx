"use client";

import { useEffect } from "react";

/**
 * Registers the PWA service worker and keeps it fresh. Because iOS PWAs cling to an old SW,
 * we actively check for updates and reload once a new SW takes control — so users always get
 * the latest build without manually reinstalling the app.
 */
export function ServiceWorkerRegister() {
  useEffect(() => {
    if (typeof window === "undefined" || !("serviceWorker" in navigator)) return;

    let reloaded = false;
    // When the new SW takes control, reload once to pick up the fresh assets.
    navigator.serviceWorker.addEventListener("controllerchange", () => {
      if (reloaded) return;
      reloaded = true;
      window.location.reload();
    });

    const onLoad = () => {
      navigator.serviceWorker
        .register("/sw.js")
        .then((reg) => {
          // Check for updates now and periodically.
          reg.update().catch(() => {});
          setInterval(() => reg.update().catch(() => {}), 60 * 60 * 1000);

          // If an updated SW is waiting, tell it to activate immediately.
          function promote(worker: ServiceWorker | null) {
            if (worker) worker.postMessage({ type: "SKIP_WAITING" });
          }
          if (reg.waiting) promote(reg.waiting);
          reg.addEventListener("updatefound", () => {
            const nw = reg.installing;
            if (!nw) return;
            nw.addEventListener("statechange", () => {
              if (nw.state === "installed" && navigator.serviceWorker.controller) {
                promote(nw);
              }
            });
          });
        })
        .catch(() => {
          /* registration failures are non-fatal */
        });
    };

    window.addEventListener("load", onLoad);
    return () => window.removeEventListener("load", onLoad);
  }, []);

  return null;
}
