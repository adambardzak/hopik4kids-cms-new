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
  startDate?: string | null;
  endDate?: string | null;
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

export interface ScheduleEntry {
  programId: string;
  programName: string;
  type: "club" | "school";
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
