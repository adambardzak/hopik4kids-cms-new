import { api } from "./api";
import type {
  Article,
  AttendanceRow,
  AttendanceStats,
  DashboardStats,
  Location,
  PageResponse,
  Program,
  Registration,
  ScheduleEntry,
  User,
} from "./types";

// --- registrations ---
export function listRegistrations(
  params: { program?: string; paymentStatus?: string; q?: string } = {},
) {
  const query = new URLSearchParams();
  if (params.program) query.set("program", params.program);
  if (params.paymentStatus) query.set("paymentStatus", params.paymentStatus);
  if (params.q) query.set("q", params.q);
  const qs = query.toString();
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

// --- schedule ---
export function listSchedule(params: { from: string; to: string; location?: string }) {
  const query = new URLSearchParams();
  query.set("from", params.from);
  query.set("to", params.to);
  if (params.location) query.set("location", params.location);
  return api<PageResponse<ScheduleEntry>>(`/admin/api/schedule?${query.toString()}`);
}

// --- dashboard ---
export function getDashboardStats() {
  return api<DashboardStats>("/admin/api/dashboard/stats");
}

// --- attendance ---
export function getAttendanceRoster(program: string, date: string) {
  return api<PageResponse<AttendanceRow>>(
    `/admin/api/attendance?program=${program}&date=${date}`,
  );
}

export function getAttendanceStats(program: string) {
  return api<AttendanceStats>(`/admin/api/attendance/stats?program=${program}`);
}
