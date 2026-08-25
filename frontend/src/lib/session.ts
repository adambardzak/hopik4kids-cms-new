import { cookies } from "next/headers";
import type { Role, Session } from "./types";

export const TOKEN_COOKIE = "h4k_token";

/** Decodes the JWT payload without verifying (backend verifies on every request). */
function decodeJwt(token: string): Record<string, unknown> | null {
  try {
    const payload = token.split(".")[1];
    const json = Buffer.from(payload, "base64url").toString("utf-8");
    return JSON.parse(json);
  } catch {
    return null;
  }
}

/** Returns the current session from the httpOnly cookie, or null. */
export async function getSession(): Promise<Session | null> {
  const store = await cookies();
  const token = store.get(TOKEN_COOKIE)?.value;
  if (!token) return null;

  const claims = decodeJwt(token);
  if (!claims) return null;

  const exp = typeof claims.exp === "number" ? claims.exp : 0;
  if (exp * 1000 < Date.now()) return null;

  return {
    userId: String(claims.sub ?? ""),
    email: String(claims.email ?? ""),
    role: String(claims.role ?? "").toLowerCase() as Role,
    name: claims.name ? String(claims.name) : undefined,
  };
}

export async function getToken(): Promise<string | null> {
  const store = await cookies();
  return store.get(TOKEN_COOKIE)?.value ?? null;
}
