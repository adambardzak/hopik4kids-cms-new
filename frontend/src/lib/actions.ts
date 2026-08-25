"use server";

import { revalidatePath } from "next/cache";
import { api, ApiRequestError } from "@/lib/api";

export interface ActionResult {
  ok: boolean;
  error?: string;
}

async function run(fn: () => Promise<unknown>, revalidate?: string): Promise<ActionResult> {
  try {
    await fn();
    if (revalidate) revalidatePath(revalidate);
    return { ok: true };
  } catch (e) {
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
