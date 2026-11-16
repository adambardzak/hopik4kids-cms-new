import { NextRequest } from "next/server";
import { BACKEND_URL } from "@/lib/api";
import { getToken } from "@/lib/session";

/** Proxies the ZIP of all approved work reports of a month (accountant / owner). */
export async function GET(request: NextRequest) {
  const token = await getToken();
  if (!token) return new Response("Unauthorized", { status: 401 });

  const period = request.nextUrl.searchParams.get("period") ?? "";
  const res = await fetch(
    `${BACKEND_URL}/admin/api/work-reports/approved.zip?period=${encodeURIComponent(period)}`,
    { headers: { Authorization: `Bearer ${token}` }, cache: "no-store" },
  );
  if (!res.ok) {
    return new Response("Za tento měsíc zatím nejsou žádné schválené výkazy.", { status: res.status });
  }

  return new Response(res.body, {
    status: 200,
    headers: {
      "Content-Type": "application/zip",
      "Content-Disposition": res.headers.get("Content-Disposition") ?? `attachment; filename=vykazy-${period}.zip`,
    },
  });
}
