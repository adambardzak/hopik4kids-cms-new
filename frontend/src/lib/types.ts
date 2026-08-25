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
