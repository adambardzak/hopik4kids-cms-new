import { NextRequest } from "next/server";
import { BACKEND_URL } from "@/lib/api";
import { getToken } from "@/lib/session";

/** Proxies the attendance PDF download, attaching the JWT from the httpOnly cookie. */
export async function GET(request: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const token = await getToken();
  if (!token) return new Response("Unauthorized", { status: 401 });

  const { id } = await params;
  const res = await fetch(`${BACKEND_URL}/admin/api/programs/${id}/attendance.pdf`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });

  if (!res.ok) return new Response("Nepodařilo se vytvořit PDF", { status: res.status });

  return new Response(res.body, {
    status: 200,
    headers: {
      "Content-Type": "application/pdf",
      "Content-Disposition": res.headers.get("Content-Disposition") ?? "inline; filename=dochazka.pdf",
    },
  });
}
