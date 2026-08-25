import { NextResponse } from "next/server";
import { BACKEND_URL } from "@/lib/api";

/** Proxies invitation acceptance to the backend (public endpoint). */
export async function POST(request: Request) {
  const body = await request.json();
  const res = await fetch(`${BACKEND_URL}/admin/auth/accept-invite`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
    cache: "no-store",
  });

  if (res.status === 204) return new NextResponse(null, { status: 204 });

  const data = await res.json().catch(() => null);
  if (!res.ok) {
    return NextResponse.json({ error: data?.error?.message ?? "Chyba" }, { status: res.status });
  }
  return NextResponse.json(data ?? { ok: true });
}
