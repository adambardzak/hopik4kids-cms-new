import { redirect } from "next/navigation";
import { getSession } from "@/lib/session";
import { navForRole } from "@/lib/nav";
import { Sidebar } from "@/components/sidebar";

export default async function AdminLayout({ children }: { children: React.ReactNode }) {
  const session = await getSession();
  if (!session) redirect("/login");

  const items = navForRole(session.role);

  return (
    <div className="flex min-h-screen">
      <Sidebar items={items} session={session} />
      <main className="flex-1 overflow-auto bg-[var(--muted)] p-8">{children}</main>
    </div>
  );
}
