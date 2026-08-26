// Service worker for the Hopík4Kids admin PWA.
// Conservative by design: it ONLY caches immutable static assets (Next build output + icons).
// Navigations, pages, API and auth are left entirely to the network/browser — the SW never
// intercepts them, so it can't break dynamic/auth-gated rendering (only static caching).
const CACHE = "hopik-admin-v2";
const STATIC_PREFIXES = ["/_next/static/", "/icons/"];

self.addEventListener("install", () => {
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (event) => {
  const { request } = event;
  if (request.method !== "GET") return;

  let url;
  try {
    url = new URL(request.url);
  } catch {
    return;
  }

  // Only handle same-origin immutable static assets. Everything else falls through to the network.
  const isStatic =
    url.origin === self.location.origin &&
    STATIC_PREFIXES.some((p) => url.pathname.startsWith(p));
  if (!isStatic) return;

  event.respondWith(
    caches.match(request).then((cached) => {
      if (cached) return cached;
      return fetch(request).then((res) => {
        // Only cache successful, basic responses.
        if (res && res.ok && res.type === "basic") {
          const copy = res.clone();
          caches.open(CACHE).then((c) => c.put(request, copy));
        }
        return res;
      });
    })
  );
});
