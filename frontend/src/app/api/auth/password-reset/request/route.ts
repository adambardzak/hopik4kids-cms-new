import { NextResponse } from "next/server";
import { BACKEND_URL } from "@/lib/api";

/** Proxies a password reset request to the backend (public). Always 202 (no enumeration). */
export async function POST(request: Request) {
  const body = await request.json();
  const res = await fetch(`${BACKEND_URL}/admin/auth/password-reset/request`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
    cache: "no-store",
  });
  if (res.status === 202) return new NextResponse(null, { status: 202 });
  const data = await res.json().catch(() => null);
  return NextResponse.json({ error: data?.error?.message ?? "Chyba" }, { status: res.status });
}
