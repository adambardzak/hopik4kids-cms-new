import { NextRequest } from "next/server";
import { BACKEND_URL } from "@/lib/api";
import { getToken } from "@/lib/session";

/** Proxies the registration export download, attaching the JWT from the httpOnly cookie. */
export async function GET(request: NextRequest) {
  const token = await getToken();
  if (!token) return new Response("Unauthorized", { status: 401 });

  const qs = request.nextUrl.searchParams.toString();
  const res = await fetch(`${BACKEND_URL}/admin/api/registrations/export${qs ? `?${qs}` : ""}`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });

  if (!res.ok) return new Response("Export selhal", { status: res.status });

  return new Response(res.body, {
    status: 200,
    headers: {
      "Content-Type": res.headers.get("Content-Type") ?? "application/octet-stream",
      "Content-Disposition": res.headers.get("Content-Disposition") ?? "attachment",
    },
  });
}
