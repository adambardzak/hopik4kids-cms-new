"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { moduleForPath } from "@/lib/nav";
import type { Role } from "@/lib/types";
import { cn } from "@/lib/utils";

/**
 * Tabs for the pages of the active module, shown at the top of the content area.
 * The sidebar shows modules; this shows the module's pages so everything stays one click away.
 */
export function ModuleTabs({ role }: { role: Role }) {
  const pathname = usePathname();
  const module = moduleForPath(pathname, role);

  // No tabs for the dashboard or single-page modules.
  if (!module || module.title === null || module.items.length < 2) return null;

  return (
    <div className="mb-6 hidden overflow-x-auto md:block">
      <div className="inline-flex gap-1 rounded-lg border border-[var(--border)] bg-[var(--background)] p-1">
        {module.items.map((item) => {
          const active =
            item.href === "/admin" ? pathname === "/admin" : pathname.startsWith(item.href);
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "whitespace-nowrap rounded-md px-4 py-2 text-sm font-medium transition-colors",
                active
                  ? "shadow-sm"
                  : "text-[var(--muted-foreground)] hover:bg-[var(--muted)] hover:text-[var(--foreground)]",
              )}
              style={active ? { background: "var(--accent)", color: "var(--accent-fg)" } : undefined}
            >
              {item.label}
            </Link>
          );
        })}
      </div>
    </div>
  );
}
