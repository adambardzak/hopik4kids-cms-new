import { NextResponse } from "next/server";
import { BACKEND_URL } from "@/lib/api";
import { getToken } from "@/lib/session";

/** Returns (creating if needed) the current user's calendar feed token. */
export async function POST() {
  const token = await getToken();
  if (!token) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const res = await fetch(`${BACKEND_URL}/admin/api/calendar/token`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });
  if (!res.ok) {
    return new NextResponse(await res.text(), { status: res.status });
  }
  const data = (await res.json()) as { token: string };
  const feedUrl = `${BACKEND_URL}/api/calendar/${data.token}.ics`;
  return NextResponse.json({ token: data.token, url: feedUrl });
}
