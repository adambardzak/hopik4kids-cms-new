import { NextRequest } from "next/server";
import { BACKEND_URL } from "@/lib/api";
import { getToken } from "@/lib/session";

/** Proxies one monthly work report PDF with the JWT from the httpOnly cookie. */
export async function GET(
  _request: NextRequest,
  { params }: { params: Promise<{ userId: string; period: string }> },
) {
  const token = await getToken();
  if (!token) return new Response("Unauthorized", { status: 401 });

  const { userId, period } = await params;
  const res = await fetch(
    `${BACKEND_URL}/admin/api/work-reports/${encodeURIComponent(userId)}/${encodeURIComponent(period)}/pdf`,
    { headers: { Authorization: `Bearer ${token}` }, cache: "no-store" },
  );
  if (!res.ok) return new Response("Výkaz se nepodařilo stáhnout", { status: res.status });

  return new Response(res.body, {
    status: 200,
    headers: {
      "Content-Type": "application/pdf",
      "Content-Disposition": res.headers.get("Content-Disposition") ?? `inline; filename=vykaz-${period}.pdf`,
    },
  });
}
