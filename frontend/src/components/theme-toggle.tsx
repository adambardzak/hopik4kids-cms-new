"use client";

import { useEffect, useState } from "react";
import { Moon, Sun } from "lucide-react";

/** Toggles dark mode; persists to localStorage and updates the <html> class. */
export function ThemeToggle({ iconOnly = false }: { iconOnly?: boolean }) {
  const [dark, setDark] = useState<boolean | null>(null);

  useEffect(() => {
    setDark(document.documentElement.classList.contains("dark"));
  }, []);

  function toggle() {
    const next = !document.documentElement.classList.contains("dark");
    document.documentElement.classList.toggle("dark", next);
    try {
      localStorage.setItem("theme", next ? "dark" : "light");
    } catch {
      /* ignore */
    }
    setDark(next);
  }

  if (dark === null) return null;

  if (iconOnly) {
    return (
      <button
        onClick={toggle}
        className="flex w-full items-center justify-center rounded-md p-2 hover:bg-[var(--muted)]"
        aria-label={dark ? "Světlý režim" : "Tmavý režim"}
      >
        {dark ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
      </button>
    );
  }

  return (
    <button
      onClick={toggle}
      className="mb-2 flex w-full items-center justify-center gap-1.5 rounded-md border border-[var(--border)] px-3 py-2 text-xs font-medium hover:bg-[var(--muted)]"
      aria-label="Přepnout tmavý režim"
    >
      {dark ? <Sun className="h-3.5 w-3.5" /> : <Moon className="h-3.5 w-3.5" />}
      {dark ? "Světlý režim" : "Tmavý režim"}
    </button>
  );
}
