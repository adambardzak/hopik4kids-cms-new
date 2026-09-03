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

  // Dashboard / single-page modules: no tabs, just the accent top edge that connects to the panel.
  if (!module || module.title === null || module.items.length < 2) {
    return <div className="hidden h-3 rounded-t-xl border-2 border-b-0 border-[var(--accent)] bg-[var(--background)] md:block" />;
  }

  return (
    <div className="hidden overflow-x-auto md:block">
      <div className="inline-flex gap-1 rounded-t-xl border-2 border-b-0 border-[var(--accent)] bg-[var(--background)] p-1.5">
        {module.items.map((item) => {
          const active =
            item.href === "/admin" ? pathname === "/admin" : pathname.startsWith(item.href);
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "whitespace-nowrap rounded-md px-4 py-1.5 text-sm font-medium transition-colors",
                active
                  ? ""
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
