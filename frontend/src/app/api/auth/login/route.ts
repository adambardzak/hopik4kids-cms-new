import { NextResponse } from "next/server";
import { cookies } from "next/headers";
import { BACKEND_URL } from "@/lib/api";
import { TOKEN_COOKIE } from "@/lib/session";

/** Proxies login to the backend and stores the JWT in an httpOnly cookie (token never in browser JS). */
export async function POST(request: Request) {
  const { email, password } = await request.json();

  const res = await fetch(`${BACKEND_URL}/admin/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
    cache: "no-store",
  });

  const data = await res.json().catch(() => null);

  if (!res.ok) {
    const message = data?.error?.message ?? "Přihlášení se nezdařilo";
    return NextResponse.json({ error: message }, { status: res.status });
  }

  const store = await cookies();
  store.set(TOKEN_COOKIE, data.token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: 60 * 60 * 24, // 24h, mirrors backend JWT TTL
  });

  return NextResponse.json({ userId: data.userId, name: data.name, role: data.role });
}
