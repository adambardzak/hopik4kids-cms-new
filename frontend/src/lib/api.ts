import { getToken, TOKEN_COOKIE } from "./session";
import type { ApiError } from "./types";
import { redirect } from "next/navigation";
import { cookies } from "next/headers";

const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

export class ApiRequestError extends Error {
  code: string;
  status: number;
  constructor(status: number, code: string, message: string) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

interface ApiOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
  auth?: boolean; // attach bearer token (default true)
}

/**
 * Server-side fetch to the Spring Boot backend. Attaches the JWT from the httpOnly cookie.
 * The token never reaches the browser. Use only in Server Components / route handlers.
 */
export async function api<T>(path: string, options: ApiOptions = {}): Promise<T> {
  const { body, auth = true, headers, ...rest } = options;

  const finalHeaders = new Headers(headers);
  if (body !== undefined && !(body instanceof FormData)) {
    finalHeaders.set("Content-Type", "application/json");
  }
  if (auth) {
    const token = await getToken();
    if (token) finalHeaders.set("Authorization", `Bearer ${token}`);
  }

  const res = await fetch(`${BACKEND_URL}${path}`, {
    ...rest,
    headers: finalHeaders,
    body: body instanceof FormData ? body : body !== undefined ? JSON.stringify(body) : undefined,
    cache: "no-store",
  });

  if (res.status === 204) return undefined as T;

  // Session expired or token rejected — clear cookie and send the user to login.
  if (res.status === 401 && auth) {
    try {
      const store = await cookies();
      store.delete(TOKEN_COOKIE);
    } catch {
      /* not in a request scope */
    }
    redirect("/login");
  }

  const text = await res.text();
  const data = text ? JSON.parse(text) : null;

  if (!res.ok) {
    const err = (data as ApiError | null)?.error;
    throw new ApiRequestError(res.status, err?.code ?? "ERROR", err?.message ?? "Chyba požadavku");
  }
  return data as T;
}

export { BACKEND_URL };
