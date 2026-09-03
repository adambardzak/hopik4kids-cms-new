import { redirect } from "next/navigation";
import { getSession } from "@/lib/session";
import { navModulesForRole } from "@/lib/nav";
import { Sidebar } from "@/components/sidebar";
import { ModuleTabs } from "@/components/module-tabs";
import { TooltipProvider } from "@/components/ui/tooltip";
import { ToastProvider } from "@/components/ui/toast";
import { ConfirmProvider } from "@/components/ui/confirm";
import { NavigationProgress } from "@/components/navigation-progress";

export default async function AdminLayout({ children }: { children: React.ReactNode }) {
  const session = await getSession();
  if (!session) redirect("/login");

  const modules = navModulesForRole(session.role);

  return (
    <ToastProvider>
      <ConfirmProvider>
        <TooltipProvider delayDuration={200}>
          <NavigationProgress />
          <div className="flex h-screen flex-col overflow-hidden md:flex-row">
            <Sidebar modules={modules} session={session} />
            <main className="flex flex-1 flex-col overflow-hidden bg-[var(--muted)] md:bg-[var(--accent)] md:p-2">
              <div className="flex min-h-0 flex-1 flex-col overflow-auto border-[var(--accent)] bg-[var(--background)] p-4 md:rounded-xl md:border md:p-8">
                <ModuleTabs role={session.role} />
                <div className="flex flex-1 flex-col pb-8 md:pb-12">{children}</div>
              </div>
            </main>
          </div>
        </TooltipProvider>
      </ConfirmProvider>
    </ToastProvider>
  );
}
