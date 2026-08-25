import { api } from "./api";
import type {
  Article,
  Location,
  PageResponse,
  Program,
  Registration,
  User,
} from "./types";

// --- registrations ---
export function listRegistrations(params: { program?: string; paymentStatus?: string } = {}) {
  const q = new URLSearchParams();
  if (params.program) q.set("program", params.program);
  if (params.paymentStatus) q.set("paymentStatus", params.paymentStatus);
  const qs = q.toString();
  return api<PageResponse<Registration>>(`/admin/api/registrations${qs ? `?${qs}` : ""}`);
}

export function getRegistration(id: string) {
  return api<Registration>(`/admin/api/registrations/${id}`);
}

// --- programs ---
export function listPrograms() {
  return api<PageResponse<Program>>("/admin/api/programs");
}

export function getProgram(id: string) {
  return api<Program>(`/admin/api/programs/${id}`);
}

// --- locations ---
export function listLocations() {
  return api<PageResponse<Location>>("/admin/api/locations");
}

// --- articles ---
export function listArticles() {
  return api<PageResponse<Article>>("/admin/api/articles");
}

export function getArticle(id: string) {
  return api<Article>(`/admin/api/articles/${id}`);
}

// --- users ---
export function listUsers() {
  return api<PageResponse<User>>("/admin/api/users");
}
