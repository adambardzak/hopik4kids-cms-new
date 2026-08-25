import type { Role } from "./types";

/** Icon keys resolved to Lucide components in the (client) Sidebar - keeps nav data serializable. */
export type IconKey = "dashboard" | "registrations" | "programs" | "locations" | "articles" | "team" | "schedule";

export interface NavItem {
  href: string;
  label: string;
  icon: IconKey;
  roles: Role[]; // roles allowed to see this item (module composition per prd §6, §12A.4)
}

/**
 * Admin navigation, composed per role (prd §6, §12A.4). Backend still enforces RBAC on every
 * API call (prd §7.5) - this only hides UI the role cannot use.
 */
export const NAV_ITEMS: NavItem[] = [
  { href: "/admin", label: "Přehled", icon: "dashboard", roles: ["owner", "admin", "trainer", "accountant", "viewer"] },
  { href: "/admin/rozvrh", label: "Rozvrh", icon: "schedule", roles: ["owner", "admin", "trainer"] },
  { href: "/admin/registrace", label: "Registrace", icon: "registrations", roles: ["owner", "admin", "trainer", "viewer"] },
  { href: "/admin/programy", label: "Programy", icon: "programs", roles: ["owner", "admin"] },
  { href: "/admin/mista", label: "Místa", icon: "locations", roles: ["owner", "admin"] },
  { href: "/admin/aktuality", label: "Aktuality", icon: "articles", roles: ["owner", "admin"] },
  { href: "/admin/tym", label: "Tým", icon: "team", roles: ["owner"] },
];

export function navForRole(role: Role): NavItem[] {
  return NAV_ITEMS.filter((item) => item.roles.includes(role));
}
