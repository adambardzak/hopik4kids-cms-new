import { NextResponse } from "next/server";
import { cookies } from "next/headers";
import { TOKEN_COOKIE } from "@/lib/session";

export async function POST() {
  const store = await cookies();
  store.delete(TOKEN_COOKIE);
  return NextResponse.json({ ok: true });
}
