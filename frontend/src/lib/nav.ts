import type { Role } from "./types";

/** Icon keys resolved to Lucide components in the (client) Sidebar - keeps nav data serializable. */
export type IconKey =
  | "dashboard" | "registrations" | "programs" | "locations" | "articles" | "team"
  | "schedule" | "attendance" | "billing" | "marketing" | "documents" | "shifts"
  | "waitlist" | "worklog" | "records" | "matching"
  | "operations" | "participants" | "content" | "settings";

export interface NavItem {
  href: string;
  label: string;
  icon: IconKey;
  roles: Role[]; // roles allowed to see this item (module composition per prd §6, §12A.4)
}

export interface NavModule {
  /** Stable id for the module. */
  id: string;
  /** Sidebar label; null = a standalone top-level link (e.g. the dashboard). */
  title: string | null;
  /** Module-level icon shown in the sidebar. */
  icon: IconKey;
  /** Accent color (hue) used for the active state + content ring. */
  accent: string;
  /** Pages belonging to the module (rendered as tabs at the top of the page). */
  items: NavItem[];
}

/**
 * Admin navigation as modules (prd §6, §12A.4). The sidebar shows the dashboard + one entry per
 * module; the module's pages appear as tabs at the top of the page. Backend still enforces RBAC
 * on every API call (prd §7.5) - this only hides UI the role cannot use.
 */
export const NAV_MODULES: NavModule[] = [
  {
    id: "dashboard",
    title: null,
    icon: "dashboard",
    accent: "#3b82f6",
    items: [
      { href: "/admin", label: "Přehled", icon: "dashboard", roles: ["owner", "admin", "trainer", "accountant", "viewer"] },
    ],
  },
  {
    id: "provoz",
    title: "Provoz",
    icon: "operations",
    accent: "#0ea5e9",
    items: [
      { href: "/admin/rozvrh", label: "Rozvrh", icon: "schedule", roles: ["owner", "admin", "trainer"] },
      { href: "/admin/dochazka", label: "Docházka", icon: "attendance", roles: ["owner", "admin", "trainer"] },
      { href: "/admin/smeny", label: "Směny", icon: "shifts", roles: ["owner", "admin", "trainer"] },
      { href: "/admin/vykazy", label: "Výkazy hodin", icon: "worklog", roles: ["owner", "admin", "trainer"] },
    ],
  },
  {
    id: "ucastnici",
    title: "Účastníci",
    icon: "participants",
    accent: "#10b981",
    items: [
      { href: "/admin/registrace", label: "Registrace", icon: "registrations", roles: ["owner", "admin", "viewer"] },
      { href: "/admin/cekaci-listina", label: "Čekací listina", icon: "waitlist", roles: ["owner", "admin"] },
      { href: "/admin/fakturace", label: "Fakturace", icon: "billing", roles: ["owner", "admin", "accountant"] },
      { href: "/admin/parovani", label: "Párování plateb", icon: "matching", roles: ["owner", "admin", "accountant"] },
      { href: "/admin/doklady", label: "Doklady", icon: "records", roles: ["owner", "admin", "accountant"] },
    ],
  },
  {
    id: "obsah",
    title: "Obsah",
    icon: "content",
    accent: "#f59e0b",
    items: [
      { href: "/admin/aktuality", label: "Aktuality", icon: "articles", roles: ["owner", "admin"] },
      { href: "/admin/dokumenty", label: "Dokumenty", icon: "documents", roles: ["owner", "admin", "trainer"] },
      { href: "/admin/marketing", label: "Marketing", icon: "marketing", roles: ["owner", "admin"] },
    ],
  },
  {
    id: "nastaveni",
    title: "Nastavení",
    icon: "settings",
    accent: "#8b5cf6",
    items: [
      { href: "/admin/programy", label: "Programy", icon: "programs", roles: ["owner", "admin"] },
      { href: "/admin/mista", label: "Místa", icon: "locations", roles: ["owner", "admin"] },
      { href: "/admin/tym", label: "Tým", icon: "team", roles: ["owner"] },
    ],
  },
];

/** Modules (with role-visible items) available to a role; empty modules are dropped. */
export function navModulesForRole(role: Role): NavModule[] {
  return NAV_MODULES.map((m) => ({
    ...m,
    items: m.items.filter((item) => item.roles.includes(role)),
  })).filter((m) => m.items.length > 0);
}

/** The module a given pathname belongs to (for tabs + active state). */
export function moduleForPath(pathname: string, role: Role): NavModule | null {
  const modules = navModulesForRole(role);
  // Longest matching href wins (so /admin doesn't shadow /admin/rozvrh).
  let best: { module: NavModule; len: number } | null = null;
  for (const m of modules) {
    for (const item of m.items) {
      const matches = item.href === "/admin" ? pathname === "/admin" : pathname.startsWith(item.href);
      if (matches && (!best || item.href.length > best.len)) {
        best = { module: m, len: item.href.length };
      }
    }
  }
  return best?.module ?? null;
}
