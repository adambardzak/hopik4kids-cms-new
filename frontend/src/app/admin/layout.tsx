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
            <main className="flex flex-1 flex-col overflow-auto bg-[var(--muted)] p-4 md:p-8">
              <ModuleTabs role={session.role} />
              <div className="h4k-fade-in flex min-h-0 flex-1 flex-col pb-10">{children}</div>
            </main>
          </div>
        </TooltipProvider>
      </ConfirmProvider>
    </ToastProvider>
  );
}
