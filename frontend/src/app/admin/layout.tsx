import { redirect } from "next/navigation";
import { getSession } from "@/lib/session";
import { navForRole } from "@/lib/nav";
import { Sidebar } from "@/components/sidebar";
import { TooltipProvider } from "@/components/ui/tooltip";

export default async function AdminLayout({ children }: { children: React.ReactNode }) {
  const session = await getSession();
  if (!session) redirect("/login");

  const items = navForRole(session.role);

  return (
    <TooltipProvider delayDuration={200}>
      <div className="flex h-screen overflow-hidden">
        <Sidebar items={items} session={session} />
        <main className="flex-1 overflow-auto bg-[var(--muted)] p-8">{children}</main>
      </div>
    </TooltipProvider>
  );
}
