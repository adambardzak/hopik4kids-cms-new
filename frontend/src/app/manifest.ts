import type { MetadataRoute } from "next";

/** PWA manifest so the admin can be installed on phones/desktop (prd: usable on the go). */
export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "Hopík4Kids — administrace",
    short_name: "Hopík4Kids",
    description: "Administrace Hopík4Kids — registrace, rozvrh, docházka, směny.",
    start_url: "/admin",
    scope: "/",
    display: "standalone",
    orientation: "portrait-primary",
    background_color: "#264664",
    theme_color: "#264664",
    lang: "cs",
    icons: [
      { src: "/icons/icon-192.png", sizes: "192x192", type: "image/png", purpose: "any" },
      { src: "/icons/icon-512.png", sizes: "512x512", type: "image/png", purpose: "any" },
      { src: "/icons/maskable-192.png", sizes: "192x192", type: "image/png", purpose: "maskable" },
      { src: "/icons/maskable-512.png", sizes: "512x512", type: "image/png", purpose: "maskable" },
    ],
  };
}
