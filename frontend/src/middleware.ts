import { NextResponse, type NextRequest } from "next/server";

const TOKEN_COOKIE = "h4k_token";

/**
 * Guards /admin routes: without a session cookie, redirect to /login.
 * Fine-grained RBAC is enforced by the backend on every API call (prd §7.5) - this is UX only.
 */
export function middleware(request: NextRequest) {
  const token = request.cookies.get(TOKEN_COOKIE)?.value;
  if (!token) {
    const url = new URL("/login", request.url);
    url.searchParams.set("next", request.nextUrl.pathname);
    return NextResponse.redirect(url);
  }
  // Expose the pathname to server components (layout resolves the active module's accent).
  const res = NextResponse.next();
  res.headers.set("x-pathname", request.nextUrl.pathname);
  return res;
}

export const config = {
  matcher: ["/admin/:path*"],
};
