/** Format a CZK amount, e.g. 1234 -> "1 234 Kč". Nullish -> "—". */
export function czk(n?: number | null): string {
  if (n == null) return "—";
  return n.toLocaleString("cs-CZ") + " Kč";
}

/** Format an ISO date string as a Czech date, e.g. "1. 9. 2026". Nullish -> "—". */
export function czDate(iso?: string | null): string {
  return iso ? new Date(iso).toLocaleDateString("cs-CZ") : "—";
}
