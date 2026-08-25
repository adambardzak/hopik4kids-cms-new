"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { LogOut, LayoutDashboard, Users2, CalendarDays, MapPin, Newspaper, ClipboardList, type LucideIcon } from "lucide-react";
import type { IconKey, NavItem } from "@/lib/nav";
import type { Session } from "@/lib/types";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";

const ICONS: Record<IconKey, LucideIcon> = {
  dashboard: LayoutDashboard,
  registrations: ClipboardList,
  programs: CalendarDays,
  locations: MapPin,
  articles: Newspaper,
  team: Users2,
};

const ROLE_LABELS: Record<string, string> = {
  owner: "Vlastník",
  admin: "Správce",
  trainer: "Trenér",
  accountant: "Účetní",
  viewer: "Náhled",
};

export function Sidebar({ items, session }: { items: NavItem[]; session: Session }) {
  const pathname = usePathname();
  const router = useRouter();

  async function logout() {
    await fetch("/api/auth/logout", { method: "POST" });
    router.replace("/login");
    router.refresh();
  }

  return (
    <aside className="flex w-64 flex-col border-r border-[var(--border)] bg-[var(--background)]">
      <div className="p-6">
        <span className="text-xl font-bold">Hopík4Kids</span>
      </div>
      <nav className="flex flex-1 flex-col gap-1 px-3">
        {items.map((item) => {
          const active = item.href === "/admin" ? pathname === item.href : pathname.startsWith(item.href);
          const Icon = ICONS[item.icon];
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center gap-3 rounded-md px-3 py-2.5 text-sm font-medium transition-colors",
                active ? "bg-[var(--primary)] text-[var(--primary-foreground)]" : "hover:bg-[var(--muted)]",
              )}
            >
              <Icon className="h-4 w-4" />
              {item.label}
            </Link>
          );
        })}
      </nav>
      <div className="border-t border-[var(--border)] p-4">
        <div className="mb-2">
          <p className="truncate text-sm font-medium">{session.name ?? session.email}</p>
          <p className="text-xs text-[var(--muted-foreground)]">{ROLE_LABELS[session.role] ?? session.role}</p>
        </div>
        <Button variant="outline" size="sm" className="w-full" onClick={logout}>
          <LogOut className="h-4 w-4" />
          Odhlásit se
        </Button>
      </div>
    </aside>
  );
}
