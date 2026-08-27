"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { LogOut, LayoutDashboard, Users2, CalendarDays, CalendarClock, ClipboardCheck, FileText, TrendingUp, BookOpen, CalendarPlus, MapPin, Newspaper, ClipboardList, Menu, X, type LucideIcon } from "lucide-react";
import type { IconKey, NavItem } from "@/lib/nav";
import type { Session } from "@/lib/types";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { NotificationToggle } from "@/components/notification-toggle";
import { ThemeToggle } from "@/components/theme-toggle";

const ICONS: Record<IconKey, LucideIcon> = {
  dashboard: LayoutDashboard,
  registrations: ClipboardList,
  programs: CalendarDays,
  locations: MapPin,
  articles: Newspaper,
  team: Users2,
  schedule: CalendarClock,
  attendance: ClipboardCheck,
  billing: FileText,
  marketing: TrendingUp,
  documents: BookOpen,
  shifts: CalendarPlus,
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
  const [open, setOpen] = useState(false);

  // Close the mobile drawer whenever the route changes.
  useEffect(() => {
    setOpen(false);
  }, [pathname]);

  // Lock body scroll while the drawer is open.
  useEffect(() => {
    document.body.style.overflow = open ? "hidden" : "";
    return () => {
      document.body.style.overflow = "";
    };
  }, [open]);

  async function logout() {
    await fetch("/api/auth/logout", { method: "POST" });
    router.replace("/login");
    router.refresh();
  }

  const nav = (
    <nav className="flex flex-1 flex-col gap-1 overflow-y-auto px-3">
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
  );

  const footer = (
    <div className="border-t border-[var(--border)] p-4">
      <div className="mb-2">
        <p className="truncate text-sm font-medium">{session.name ?? session.email}</p>
        <p className="text-xs text-[var(--muted-foreground)]">{ROLE_LABELS[session.role] ?? session.role}</p>
      </div>
      {(session.role === "owner" || session.role === "admin") && <NotificationToggle />}
      <ThemeToggle />
      <Button variant="outline" size="sm" className="w-full" onClick={logout}>
        <LogOut className="h-4 w-4" />
        Odhlásit se
      </Button>
    </div>
  );

  const brand = (
    <div className="flex items-center gap-2.5 p-6">
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img src="/logo.svg" alt="Hopík4Kids" className="h-9 w-auto" />
      <span className="text-lg font-bold">Hopík4Kids</span>
    </div>
  );

  return (
    <>
      {/* Mobile top bar with hamburger (hidden on md+). Respects the notch / dynamic island. */}
      <div
        className="sticky top-0 z-30 flex items-center gap-2 border-b border-[var(--border)] bg-[var(--background)] p-3 md:hidden"
        style={{
          paddingTop: "max(0.75rem, env(safe-area-inset-top))",
          paddingLeft: "max(0.75rem, env(safe-area-inset-left))",
          paddingRight: "max(0.75rem, env(safe-area-inset-right))",
        }}
      >
        <button
          onClick={() => setOpen(true)}
          className="rounded-md p-2 hover:bg-[var(--muted)]"
          aria-label="Otevřít menu"
        >
          <Menu className="h-5 w-5" />
        </button>
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img src="/logo.svg" alt="Hopík4Kids" className="h-7 w-auto" />
        <span className="font-bold">Hopík4Kids</span>
      </div>

      {/* Desktop sidebar (always visible on md+) */}
      <aside className="hidden h-full w-64 shrink-0 flex-col border-r border-[var(--border)] bg-[var(--background)] md:flex">
        {brand}
        {nav}
        {footer}
      </aside>

      {/* Mobile drawer + overlay */}
      {open && (
        <div className="fixed inset-0 z-50 md:hidden">
          <div
            className="absolute inset-0 bg-black/40"
            onClick={() => setOpen(false)}
            aria-hidden
          />
          <aside
            className="absolute left-0 top-0 flex h-full w-72 max-w-[85vw] flex-col border-r border-[var(--border)] bg-[var(--background)] shadow-xl"
            style={{
              paddingTop: "env(safe-area-inset-top)",
              paddingBottom: "env(safe-area-inset-bottom)",
              paddingLeft: "env(safe-area-inset-left)",
            }}
          >
            <div className="flex items-center justify-between pr-2">
              {brand}
              <button
                onClick={() => setOpen(false)}
                className="rounded-md p-2 hover:bg-[var(--muted)]"
                aria-label="Zavřít menu"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            {nav}
            {footer}
          </aside>
        </div>
      )}
    </>
  );
}
