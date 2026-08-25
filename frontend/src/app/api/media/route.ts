import { NextRequest, NextResponse } from "next/server";
import { BACKEND_URL } from "@/lib/api";
import { getToken } from "@/lib/session";

/** Proxies media upload (multipart) to the backend with the cookie JWT. */
export async function POST(request: NextRequest) {
  const token = await getToken();
  if (!token) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const formData = await request.formData();
  const res = await fetch(`${BACKEND_URL}/admin/api/media`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    body: formData,
    cache: "no-store",
  });

  const data = await res.json().catch(() => null);
  if (!res.ok) {
    return NextResponse.json({ error: data?.error?.message ?? "Nahrání selhalo" }, { status: res.status });
  }
  return NextResponse.json(data);
}
