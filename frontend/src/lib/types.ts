export type Role = "owner" | "admin" | "trainer" | "accountant" | "viewer";

export interface Session {
  userId: string;
  email: string;
  role: Role;
  name?: string;
}

export interface PageResponse<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
}

export interface ApiError {
  error: { code: string; message: string };
}

export interface Program {
  id: string;
  type: "club" | "school" | "camp";
  name: string;
  slug?: string | null;
  locationId?: string | null;
  price: number;
  capacity?: number | null;
  spotsTaken: number;
  accessMode: "public" | "notice_only" | "code" | "unlisted";
  restrictionNote?: string | null;
  hasAccessCode: boolean;
  shirtPolicy: "none" | "optional" | "required";
  status: "active" | "hidden" | "archived";
  weekday?: number | null;
  time?: string | null;
  schoolPart?: "morning" | "afternoon" | null;
  validFrom?: string | null;
  validTo?: string | null;
  durationMin?: number | null;
  trainersNeeded?: number | null;
  startDate?: string | null;
  endDate?: string | null;
  trainerIds?: string[];
}

export interface Trainer {
  id: string;
  name: string;
  email: string;
}

export type DocumentCategory = "pravidla" | "metodika" | "checklist" | "formular" | "ostatni";

export interface ShiftSignupTrainer {
  signupId: string;
  trainerId: string;
  trainerName: string;
  status: string;
}

export interface ShiftSlot {
  programId: string;
  programName: string;
  type: string;
  date: string;
  startTime?: string | null;
  endTime?: string | null;
  locationName?: string | null;
  trainersNeeded: number;
  approvedCount: number;
  pendingCount: number;
  mySignupId?: string | null;
  myStatus?: string | null;
  signups: ShiftSignupTrainer[];
}

export interface DocumentItem {
  id: string;
  title: string;
  category: DocumentCategory;
  fileUrl?: string | null;
  fileName?: string | null;
  content?: string | null;
  visibility: "trainers" | "admin";
  sortOrder: number;
}

export interface Location {
  id: string;
  name: string;
  kind: "kindergarten" | "school" | "venue";
  address?: string | null;
  contactName?: string | null;
  contactPhone?: string | null;
  contactEmail?: string | null;
  note?: string | null;
}

export interface Registration {
  id: string;
  programId: string;
  programName: string;
  programType: string;
  programLocationName?: string | null;
  programWeekday?: number | null;
  programTime?: string | null;
  programSchoolPart?: string | null;
  childName: string;
  birthDate: string;
  personalId: string;
  childAddress: string;
  healthInsurance: string;
  className?: string | null;
  parentName: string;
  parentPhone: string;
  parentEmail: string;
  secondParentName?: string | null;
  secondParentPhone?: string | null;
  wantsShirt: boolean;
  shirtSize?: string | null;
  nickName?: string | null;
  allergies?: string | null;
  note?: string | null;
  consentPersonalData: boolean;
  consentMedia: boolean;
  paymentStatus: "unpaid" | "paid" | "cancelled";
  priceSnapshot: number;
  status: "active" | "cancelled";
  source?: string | null;
  createdAt: string;
}

export interface Article {
  id: string;
  title: string;
  slug: string;
  excerpt?: string | null;
  content?: string | null;
  coverId?: string | null;
  coverUrl?: string | null;
  publishedAt?: string | null;
  published: boolean;
}

export interface User {
  id: string;
  name: string;
  email: string;
  role: Role;
  status: "invited" | "active" | "disabled";
  phone?: string | null;
  color?: string | null;
  lastLoginAt?: string | null;
}

export interface Media {
  id: string;
  url: string;
  alt?: string | null;
  width?: number | null;
  height?: number | null;
}

export interface UnderfilledProgram {
  id: string;
  name: string;
  type: string;
  capacity: number;
  spotsTaken: number;
  occupancyPct: number;
}

export interface DashboardStats {
  registrationsToday: number;
  registrationsThisWeek: number;
  activePrograms: number;
  totalActiveRegistrations: number;
  totalCapacity: number;
  totalSpotsTaken: number;
  confirmedRevenue: number;
  expectedRevenue: number;
  potentialRevenue: number;
  unpaidCount: number;
  unpaidAmount: number;
  withoutMediaConsent: number;
  underfilled: UnderfilledProgram[];
}

export type AttendanceStatus = "present" | "excused" | "absent";

export interface AttendanceRow {
  childId: string;
  childName: string;
  status: AttendanceStatus | null;
  note?: string | null;
}

export interface AttendanceStats {
  children: {
    childId: string;
    childName: string;
    present: number;
    excused: number;
    absent: number;
    totalRecorded: number;
  }[];
  lessons: {
    date: string;
    present: number;
    excused: number;
    absent: number;
  }[];
}

export interface Invoice {
  id: string;
  invoiceNumber: string;
  registrationId: string;
  type: string;
  payerName: string;
  payerAddress?: string | null;
  payerEmail?: string | null;
  items: string; // JSON array
  totalAmount: number;
  programAmount: number;
  shirtAmount: number;
  issueDate: string;
  dueDate: string;
  variableSymbol: string;
  status: "unpaid" | "paid" | "cancelled";
  paidAt?: string | null;
}

export interface SupplierSettings {
  id?: string;
  name: string;
  ico?: string | null;
  dic?: string | null;
  address?: string | null;
  iban?: string | null;
  accountNumber?: string | null;
  web?: string | null;
  email?: string | null;
  defaultDueDays: number;
  footerText?: string | null;
}

export interface WaitlistEntry {
  id: string;
  programId: string;
  programName: string;
  childName: string;
  parentName: string;
  parentPhone: string;
  parentEmail: string;
  note?: string | null;
  status: "waiting" | "offered" | "converted" | "cancelled";
  createdAt: string;
}

export interface MarketingStats {
  sources: { source: string; count: number }[];
  registrationsWithoutSource: number;
  returningChildren: number;
  totalDistinctChildren: number;
  retentionPct: number;
  clubsNotInCamp: { childName: string; parentName: string; parentPhone: string; parentEmail: string }[];
  campChildren: number;
}

export interface ScheduleEntry {
  programId: string;
  programName: string;
  type: "club" | "school";
  status?: "active" | "hidden" | "archived";
  overrideId?: string | null;
  overrideType?: "cancelled" | "moved" | "one_off" | null;
  title?: string | null;
  date: string; // ISO date
  weekday: number; // 1=Mon..7=Sun
  startTime: string; // "HH:MM"
  endTime?: string | null;
  durationMin?: number | null;
  schoolPart?: "morning" | "afternoon" | null;
  locationId?: string | null;
  locationName?: string | null;
  locationAddress?: string | null;
  contactName?: string | null;
  contactPhone?: string | null;
  contactEmail?: string | null;
  validFrom?: string | null;
  validTo?: string | null;
  capacity?: number | null;
  spotsTaken: number;
}
