import { api } from "./api";
import type {
  Article,
  AttendanceRow,
  AttendanceStats,
  DashboardStats,
  DocumentItem,
  Invoice,
  Location,
  MarketingStats,
  PageResponse,
  Program,
  Registration,
  ScheduleEntry,
  ShiftSlot,
  SupplierSettings,
  Trainer,
  User,
  WaitlistEntry,
  WorkLog,
  WorkLogSummary,
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

// --- billing ---
export function listInvoices(filters?: { from?: string; to?: string; status?: string; type?: string }) {
  const q = new URLSearchParams();
  if (filters?.from) q.set("from", filters.from);
  if (filters?.to) q.set("to", filters.to);
  if (filters?.status) q.set("status", filters.status);
  if (filters?.type) q.set("type", filters.type);
  const qs = q.toString();
  return api<PageResponse<Invoice>>(`/admin/api/billing/invoices${qs ? `?${qs}` : ""}`);
}

export function getSupplierSettings() {
  return api<SupplierSettings>("/admin/api/billing/supplier");
}

export function aresLookup(ico: string) {
  return api<{ ico: string; name?: string | null; address?: string | null; dic?: string | null }>(
    `/admin/api/billing/ares/${encodeURIComponent(ico)}`,
  );
}

// --- bulk email ---
export function getBulkRecipients(program: string) {
  return api<PageResponse<string>>(`/admin/api/bulk-email/recipients?program=${program}`);
}

// --- waitlist ---
export function listWaitlist(program: string) {
  return api<PageResponse<WaitlistEntry>>(`/admin/api/waitlist?program=${program}`);
}

// --- marketing ---
export function getMarketingStats() {
  return api<MarketingStats>("/admin/api/marketing/stats");
}

// --- trainers ---
export function listTrainers() {
  return api<PageResponse<Trainer>>("/admin/api/trainers");
}

// --- documents ---
export function listDocuments() {
  return api<PageResponse<DocumentItem>>("/admin/api/documents");
}

// --- shifts (prd §7.4) ---
export function listOpenShifts(from: string, to: string) {
  return api<PageResponse<ShiftSlot>>(`/admin/api/shifts/open?from=${from}&to=${to}`);
}

export function listMyShifts(from: string, to: string) {
  return api<PageResponse<ShiftSlot>>(`/admin/api/shifts/mine?from=${from}&to=${to}`);
}

// --- work logs (výkazy práce) ---
export function listWorkLogs(from?: string, to?: string) {
  const q = new URLSearchParams();
  if (from) q.set("from", from);
  if (to) q.set("to", to);
  const qs = q.toString();
  return api<PageResponse<WorkLog>>(`/admin/api/work-logs${qs ? `?${qs}` : ""}`);
}

export function getWorkLogSummary(from?: string, to?: string) {
  const q = new URLSearchParams();
  if (from) q.set("from", from);
  if (to) q.set("to", to);
  const qs = q.toString();
  return api<PageResponse<WorkLogSummary>>(`/admin/api/work-logs/summary${qs ? `?${qs}` : ""}`);
}
