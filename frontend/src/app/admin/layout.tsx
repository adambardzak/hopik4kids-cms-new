import { redirect } from "next/navigation";
import { getSession } from "@/lib/session";
import { navGroupsForRole } from "@/lib/nav";
import { Sidebar } from "@/components/sidebar";
import { TooltipProvider } from "@/components/ui/tooltip";
import { ToastProvider } from "@/components/ui/toast";
import { ConfirmProvider } from "@/components/ui/confirm";

export default async function AdminLayout({ children }: { children: React.ReactNode }) {
  const session = await getSession();
  if (!session) redirect("/login");

  const groups = navGroupsForRole(session.role);

  return (
    <ToastProvider>
      <ConfirmProvider>
        <TooltipProvider delayDuration={200}>
          <div className="flex h-screen flex-col overflow-hidden md:flex-row">
            <Sidebar groups={groups} session={session} />
            <main className="h4k-fade-in flex-1 overflow-auto bg-[var(--muted)] p-4 md:p-8">{children}</main>
          </div>
        </TooltipProvider>
      </ConfirmProvider>
    </ToastProvider>
  );
}
