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
    <div className="mb-5 hidden gap-1 overflow-x-auto border-b border-[var(--border)] md:flex">
      {module.items.map((item) => {
        const active =
          item.href === "/admin" ? pathname === "/admin" : pathname.startsWith(item.href);
        return (
          <Link
            key={item.href}
            href={item.href}
            className={cn(
              "-mb-px whitespace-nowrap border-b-2 px-4 py-2.5 text-sm font-medium transition-colors",
              active
                ? "border-[var(--primary)] text-[var(--primary)]"
                : "border-transparent text-[var(--muted-foreground)] hover:text-[var(--foreground)]",
            )}
          >
            {item.label}
          </Link>
        );
      })}
    </div>
  );
}
