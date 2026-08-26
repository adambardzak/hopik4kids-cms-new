"use server";

import { revalidatePath } from "next/cache";
import { api, ApiRequestError } from "@/lib/api";
import type { WaitlistEntry } from "@/lib/types";

export interface ActionResult {
  ok: boolean;
  error?: string;
}

/** Next's redirect() throws a special error whose digest starts with NEXT_REDIRECT — must re-throw. */
function isRedirect(e: unknown): boolean {
  return (
    typeof e === "object" &&
    e !== null &&
    "digest" in e &&
    typeof (e as { digest: unknown }).digest === "string" &&
    (e as { digest: string }).digest.startsWith("NEXT_REDIRECT")
  );
}

async function run(fn: () => Promise<unknown>, revalidate?: string): Promise<ActionResult> {
  try {
    await fn();
    if (revalidate) revalidatePath(revalidate);
    return { ok: true };
  } catch (e) {
    if (isRedirect(e)) throw e; // let session-expiry redirect propagate
    if (e instanceof ApiRequestError) return { ok: false, error: e.message };
    return { ok: false, error: "Neočekávaná chyba" };
  }
}

// --- registrations ---
export async function cancelRegistration(id: string): Promise<ActionResult> {
  return run(() => api(`/admin/api/registrations/${id}/cancel`, { method: "POST" }), "/admin/registrace");
}

export async function setPaymentStatus(id: string, status: string): Promise<ActionResult> {
  return run(
    () => api(`/admin/api/registrations/${id}/payment-status?status=${status}`, { method: "POST" }),
    "/admin/registrace",
  );
}

// --- programs ---
export async function saveProgram(id: string | null, body: unknown): Promise<ActionResult> {
  return run(
    () =>
      id
        ? api(`/admin/api/programs/${id}`, { method: "PUT", body })
        : api(`/admin/api/programs`, { method: "POST", body }),
    "/admin/programy",
  );
}

export async function deleteProgram(id: string): Promise<ActionResult> {
  return run(() => api(`/admin/api/programs/${id}`, { method: "DELETE" }), "/admin/programy");
}

// --- articles ---
export async function saveArticle(id: string | null, body: unknown): Promise<ActionResult> {
  return run(
    () =>
      id
        ? api(`/admin/api/articles/${id}`, { method: "PUT", body })
        : api(`/admin/api/articles`, { method: "POST", body }),
    "/admin/aktuality",
  );
}

export async function deleteArticle(id: string): Promise<ActionResult> {
  return run(() => api(`/admin/api/articles/${id}`, { method: "DELETE" }), "/admin/aktuality");
}

// --- locations ---
export async function saveLocation(id: string | null, body: unknown): Promise<ActionResult> {
  return run(
    () =>
      id
        ? api(`/admin/api/locations/${id}`, { method: "PUT", body })
        : api(`/admin/api/locations`, { method: "POST", body }),
    "/admin/mista",
  );
}

export async function deleteLocation(id: string): Promise<ActionResult> {
  return run(() => api(`/admin/api/locations/${id}`, { method: "DELETE" }), "/admin/mista");
}

// --- users ---
export async function inviteUser(email: string, role: string): Promise<ActionResult> {
  return run(() => api(`/admin/api/users/invite`, { method: "POST", body: { email, role } }), "/admin/tym");
}

export async function changeUserRole(id: string, role: string): Promise<ActionResult> {
  return run(() => api(`/admin/api/users/${id}/role?role=${role}`, { method: "POST" }), "/admin/tym");
}

export async function deactivateUser(id: string): Promise<ActionResult> {
  return run(() => api(`/admin/api/users/${id}/deactivate`, { method: "POST" }), "/admin/tym");
}

// --- attendance ---
export async function saveAttendance(
  program: string,
  date: string,
  entries: { childId: string; status: string | null; note?: string | null }[],
): Promise<ActionResult> {
  return run(() =>
    api(`/admin/api/attendance?program=${program}&date=${date}`, {
      method: "PUT",
      body: { entries },
    }),
  );
}

export async function fetchAttendanceRoster(program: string, date: string) {
  const { getAttendanceRoster } = await import("@/lib/admin-data");
  try {
    const res = await getAttendanceRoster(program, date);
    return { ok: true as const, items: res.items };
  } catch {
    return { ok: false as const, items: [] };
  }
}

export async function fetchAttendanceStats(program: string) {
  const { getAttendanceStats } = await import("@/lib/admin-data");
  try {
    const stats = await getAttendanceStats(program);
    return { ok: true as const, stats };
  } catch {
    return { ok: false as const, stats: null };
  }
}

// --- billing ---
export async function createInvoice(registrationId: string): Promise<ActionResult> {
  return run(
    () => api(`/admin/api/billing/invoices?registration=${registrationId}`, { method: "POST" }),
    "/admin/fakturace",
  );
}

export async function markInvoicePaid(id: string): Promise<ActionResult> {
  return run(() => api(`/admin/api/billing/invoices/${id}/paid`, { method: "POST" }), "/admin/fakturace");
}

export async function cancelInvoice(id: string): Promise<ActionResult> {
  return run(() => api(`/admin/api/billing/invoices/${id}/cancel`, { method: "POST" }), "/admin/fakturace");
}

export async function saveSupplierSettings(body: unknown): Promise<ActionResult> {
  return run(() => api(`/admin/api/billing/supplier`, { method: "PUT", body }), "/admin/fakturace");
}

/** Create invoice and return its id (for opening the PDF). */
export async function createInvoiceReturningId(
  registrationId: string,
): Promise<{ ok: boolean; id?: string; error?: string }> {
  try {
    const inv = await api<{ id: string }>(
      `/admin/api/billing/invoices?registration=${registrationId}`,
      { method: "POST" },
    );
    return { ok: true, id: inv.id };
  } catch (e) {
    if (isRedirect(e)) throw e;
    if (e instanceof ApiRequestError) return { ok: false, error: e.message };
    return { ok: false, error: "Neočekávaná chyba" };
  }
}

export async function lookupAres(ico: string): Promise<{
  ok: boolean;
  data?: { name?: string | null; address?: string | null; dic?: string | null };
  error?: string;
}> {
  try {
    const { aresLookup } = await import("@/lib/admin-data");
    const data = await aresLookup(ico);
    return { ok: true, data };
  } catch (e) {
    if (isRedirect(e)) throw e;
    if (e instanceof ApiRequestError) return { ok: false, error: e.message };
    return { ok: false, error: "Nepodařilo se načíst z ARESu" };
  }
}

export async function sendInvoiceEmail(id: string): Promise<ActionResult> {
  return run(() => api(`/admin/api/billing/invoices/${id}/send`, { method: "POST" }), "/admin/fakturace");
}

// --- bulk email ---
export async function fetchBulkRecipients(program: string): Promise<{ ok: boolean; emails: string[] }> {
  try {
    const { getBulkRecipients } = await import("@/lib/admin-data");
    const res = await getBulkRecipients(program);
    return { ok: true, emails: res.items };
  } catch {
    return { ok: false, emails: [] };
  }
}

export async function sendBulkEmail(
  program: string,
  subject: string,
  body: string,
): Promise<{ ok: boolean; result?: { total: number; sent: number; failed: number }; error?: string }> {
  try {
    const result = await api<{ total: number; sent: number; failed: number }>("/admin/api/bulk-email", {
      method: "POST",
      body: { program, subject, body },
    });
    return { ok: true, result };
  } catch (e) {
    if (isRedirect(e)) throw e;
    if (e instanceof ApiRequestError) return { ok: false, error: e.message };
    return { ok: false, error: "Odeslání selhalo" };
  }
}

// --- waitlist ---

export async function fetchWaitlist(program: string): Promise<{ ok: boolean; items: WaitlistEntry[] }> {
  try {
    const { listWaitlist } = await import("@/lib/admin-data");
    const res = await listWaitlist(program);
    return { ok: true, items: res.items };
  } catch {
    return { ok: false, items: [] };
  }
}

export async function setWaitlistStatus(id: string, status: string): Promise<ActionResult> {
  return run(() => api(`/admin/api/waitlist/${id}/status?status=${status}`, { method: "POST" }), "/admin/registrace");
}

export async function deleteWaitlistEntry(id: string): Promise<ActionResult> {
  return run(() => api(`/admin/api/waitlist/${id}`, { method: "DELETE" }), "/admin/registrace");
}

// --- documents ---
export async function saveDocument(id: string | null, body: unknown): Promise<ActionResult> {
  return run(
    () =>
      id
        ? api(`/admin/api/documents/${id}`, { method: "PUT", body })
        : api(`/admin/api/documents`, { method: "POST", body }),
    "/admin/dokumenty",
  );
}

export async function deleteDocument(id: string): Promise<ActionResult> {
  return run(() => api(`/admin/api/documents/${id}`, { method: "DELETE" }), "/admin/dokumenty");
}

// --- shifts (prd §7.4) ---
export async function signupShift(programId: string, date: string): Promise<ActionResult> {
  return run(() => api(`/admin/api/shifts/signup`, { method: "POST", body: { programId, date } }), "/admin/smeny");
}

export async function cancelShift(signupId: string): Promise<ActionResult> {
  return run(() => api(`/admin/api/shifts/${signupId}`, { method: "DELETE" }), "/admin/smeny");
}

export async function approveShift(signupId: string): Promise<ActionResult> {
  return run(() => api(`/admin/api/shifts/${signupId}/approve`, { method: "POST" }), "/admin/smeny");
}

export async function rejectShift(signupId: string): Promise<ActionResult> {
  return run(() => api(`/admin/api/shifts/${signupId}/reject`, { method: "POST" }), "/admin/smeny");
}

// --- schedule overrides (prd §7.4) ---
export async function cancelLesson(programId: string, originalDate: string, note?: string): Promise<ActionResult> {
  return run(() => api(`/admin/api/schedule/overrides/cancel`, { method: "POST", body: { programId, originalDate, note } }), "/admin/rozvrh");
}

export async function moveLesson(body: {
  programId: string; originalDate: string; newDate: string; newTime: string; durationMin?: number | null; locationId?: string | null; note?: string;
}): Promise<ActionResult> {
  return run(() => api(`/admin/api/schedule/overrides/move`, { method: "POST", body }), "/admin/rozvrh");
}

export async function addOneOffLesson(body: {
  programId?: string | null; title?: string | null; date: string; time: string; durationMin?: number | null; locationId?: string | null; note?: string;
}): Promise<ActionResult> {
  return run(() => api(`/admin/api/schedule/overrides/one-off`, { method: "POST", body }), "/admin/rozvrh");
}

export async function deleteOverride(id: string): Promise<ActionResult> {
  return run(() => api(`/admin/api/schedule/overrides/${id}`, { method: "DELETE" }), "/admin/rozvrh");
}
