import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  reactStrictMode: true,
  // Standalone output for Docker; not needed (and skipped) on Vercel.
  ...(process.env.VERCEL ? {} : { output: "standalone" as const }),
};

export default nextConfig;
