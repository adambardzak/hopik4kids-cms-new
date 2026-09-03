"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { LogOut, LayoutDashboard, Users2, CalendarDays, CalendarClock, ClipboardCheck, FileText, TrendingUp, BookOpen, CalendarPlus, MapPin, Newspaper, ClipboardList, Menu, X, Hourglass, Clock, Receipt, ArrowLeftRight, CalendarRange, Wallet, FileEdit, Settings, type LucideIcon } from "lucide-react";
import type { IconKey, NavModule } from "@/lib/nav";
import { moduleForPath } from "@/lib/nav";
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
  waitlist: Hourglass,
  worklog: Clock,
  records: Receipt,
  matching: ArrowLeftRight,
  operations: CalendarRange,
  participants: Wallet,
  content: FileEdit,
  settings: Settings,
};

const ROLE_LABELS: Record<string, string> = {
  owner: "Vlastník",
  admin: "Správce",
  trainer: "Trenér",
  accountant: "Účetní",
  viewer: "Náhled",
};

export function Sidebar({ modules, session }: { modules: NavModule[]; session: Session }) {
  const pathname = usePathname();
  const router = useRouter();
  const [open, setOpen] = useState(false);

  const activeModule = moduleForPath(pathname, session.role);

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

  // Desktop: compact module list (module pages become tabs at the top of the page).
  const nav = (
    <nav className="flex flex-1 flex-col gap-1 overflow-y-auto py-2 pl-2">
      {modules.map((m) => {
        // The dashboard is a plain link; other modules link to their first page.
        const target = m.items[0]?.href ?? "/admin";
        const Icon = ICONS[m.icon];
        const active = activeModule?.id === m.id;
        const label = m.title ?? m.items[0]?.label ?? "";
        return (
          <Link
            key={m.id}
            href={target}
            title={label}
            className={cn(
              "flex items-center gap-3 py-2.5 pl-3 pr-3 text-sm font-medium transition-colors",
              active
                ? "h4k-nav-active rounded-l-xl rounded-r-none" // full-bleed right so it meets the accent gutter
                : "mr-2 rounded-lg hover:bg-[var(--muted)]",
            )}
            style={active ? { background: "var(--accent)", color: "var(--accent-fg)", marginRight: "-2px" } : undefined}
          >
            <Icon className="h-5 w-5 shrink-0" />
            <span className="-translate-x-2 whitespace-nowrap opacity-0 transition-all duration-300 ease-out group-hover:translate-x-0 group-hover:opacity-100">
              {label}
            </span>
          </Link>
        );
      })}
    </nav>
  );

  // Mobile drawer: full flat list grouped by module (no tabs — everything is one tap here).
  const mobileNav = (
    <nav className="flex flex-1 flex-col gap-3 overflow-y-auto px-3 py-2">
      {modules.map((m) => (
        <div key={m.id} className="flex flex-col gap-1">
          {m.title && (
            <p className="px-3 pb-1 pt-1 text-xs font-semibold uppercase tracking-wider text-[var(--muted-foreground)]">
              {m.title}
            </p>
          )}
          {m.items.map((item) => {
            const active =
              item.href === "/admin" ? pathname === "/admin" : pathname.startsWith(item.href);
            const Icon = ICONS[item.icon];
            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "flex items-center gap-3 rounded-md px-3 py-2.5 text-sm font-medium transition-colors",
                  active ? "" : "hover:bg-[var(--muted)]",
                )}
                style={active ? { background: "var(--accent)", color: "var(--accent-fg)" } : undefined}
              >
                <Icon className="h-4 w-4" />
                {item.label}
              </Link>
            );
          })}
        </div>
      ))}
    </nav>
  );

  const footer = (
    <div className="border-t border-[var(--border)] p-4">
      <div className="hidden group-hover:block">
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
      <button
        onClick={logout}
        title="Odhlásit se"
        aria-label="Odhlásit se"
        className="flex w-full items-center justify-center rounded-md p-2 hover:bg-[var(--muted)] group-hover:hidden"
      >
        <LogOut className="h-5 w-5" />
      </button>
    </div>
  );

  const mobileFooter = (
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
    <div className="flex items-center gap-2.5 px-4 py-6">
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img src="/logo.svg" alt="Hopík4Kids" className="h-9 w-auto shrink-0" />
      <span className="-translate-x-2 whitespace-nowrap text-lg font-bold opacity-0 transition-all duration-300 ease-out group-hover:translate-x-0 group-hover:opacity-100">
        Hopík4Kids
      </span>
    </div>
  );

  const mobileBrand = (
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

      {/* Desktop sidebar: collapsed icon rail that expands on hover, pushing content aside. */}
      <aside className="group hidden h-full w-16 shrink-0 flex-col overflow-hidden bg-[var(--background)] transition-[width] duration-300 ease-[cubic-bezier(0.4,0,0.2,1)] hover:w-64 md:flex">
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
            className="h4k-drawer absolute left-0 top-0 flex h-full w-72 max-w-[85vw] flex-col border-r border-[var(--border)] bg-[var(--background)] shadow-xl"
            style={{
              paddingTop: "env(safe-area-inset-top)",
              paddingBottom: "env(safe-area-inset-bottom)",
              paddingLeft: "env(safe-area-inset-left)",
            }}
          >
            <div className="flex items-center justify-between pr-2">
              {mobileBrand}
              <button
                onClick={() => setOpen(false)}
                className="rounded-md p-2 hover:bg-[var(--muted)]"
                aria-label="Zavřít menu"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            {mobileNav}
            {mobileFooter}
          </aside>
        </div>
      )}
    </>
  );
}
