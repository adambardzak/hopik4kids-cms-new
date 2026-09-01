"use client";

import { useEffect, useRef, useState } from "react";
import { usePathname } from "next/navigation";

/**
 * Thin top progress bar that appears on route navigation (GitHub/YouTube style). Gives instant
 * feedback while a server component page loads, so navigation never feels frozen.
 *
 * Starts when an internal <a>/link is clicked; completes when the pathname changes (new page ready).
 */
export function NavigationProgress() {
  const pathname = usePathname();
  const [visible, setVisible] = useState(false);
  const [width, setWidth] = useState(0);
  const timers = useRef<ReturnType<typeof setTimeout>[]>([]);

  function clearTimers() {
    timers.current.forEach(clearTimeout);
    timers.current = [];
  }

  function start() {
    clearTimers();
    setVisible(true);
    setWidth(8);
    // Creep forward while loading, easing toward ~90% but never finishing.
    const steps: [number, number][] = [
      [120, 25],
      [300, 45],
      [600, 65],
      [1000, 80],
      [1800, 90],
    ];
    for (const [delay, target] of steps) {
      timers.current.push(setTimeout(() => setWidth((w) => (w < target ? target : w)), delay));
    }
  }

  function finish() {
    clearTimers();
    setWidth(100);
    timers.current.push(
      setTimeout(() => {
        setVisible(false);
        setWidth(0);
      }, 250),
    );
  }

  // Complete the bar whenever the route actually changes.
  useEffect(() => {
    if (visible) finish();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pathname]);

  // Trigger the bar on clicks that lead to an internal navigation.
  useEffect(() => {
    function onClick(e: MouseEvent) {
      if (e.defaultPrevented || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) {
        return;
      }
      const anchor = (e.target as HTMLElement)?.closest("a");
      if (!anchor) return;
      const href = anchor.getAttribute("href");
      const target = anchor.getAttribute("target");
      if (
        !href ||
        href.startsWith("http") ||
        href.startsWith("#") ||
        href.startsWith("mailto:") ||
        href.startsWith("tel:") ||
        href.startsWith("webcal:") ||
        target === "_blank" ||
        anchor.hasAttribute("download")
      ) {
        return;
      }
      // Same-URL clicks won't change pathname → don't get stuck.
      const dest = new URL(href, window.location.href);
      if (dest.pathname === window.location.pathname && dest.search === window.location.search) {
        return;
      }
      start();
    }

    document.addEventListener("click", onClick, { capture: true });
    return () => document.removeEventListener("click", onClick, { capture: true } as EventListenerOptions);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (!visible) return null;

  return (
    <div className="pointer-events-none fixed inset-x-0 top-0 z-[200] h-0.5">
      <div
        className="h-full bg-[var(--primary)] transition-[width] duration-200 ease-out"
        style={{ width: `${width}%`, boxShadow: "0 0 8px var(--primary)" }}
      />
    </div>
  );
}
