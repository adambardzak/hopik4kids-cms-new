import { redirect } from "next/navigation";
import { headers } from "next/headers";
import { getSession } from "@/lib/session";
import { navModulesForRole, moduleForPath } from "@/lib/nav";
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

  // Resolve the active module's accent from the request path (middleware sets x-pathname).
  const pathname = (await headers()).get("x-pathname") ?? "";
  const activeModule = moduleForPath(pathname, session.role);
  const accent = activeModule?.accent ?? "#1A2B47";
  const accentFg = activeModule?.accentFg ?? "#ffffff";

  return (
    <ToastProvider>
      <ConfirmProvider>
        <TooltipProvider delayDuration={200}>
          <NavigationProgress />
          <div
            className="flex h-screen flex-col overflow-hidden md:flex-row"
            style={{ ["--accent" as string]: accent, ["--accent-fg" as string]: accentFg }}
          >
            <Sidebar modules={modules} session={session} />
            <main className="flex flex-1 flex-col overflow-hidden bg-[var(--muted)] p-3 md:p-4">
              <div className="flex min-h-0 flex-1 flex-col overflow-auto rounded-xl border-2 border-[var(--accent)] bg-[var(--background)] p-4 pb-16 md:p-8 md:pb-20">
                <ModuleTabs role={session.role} />
                <div className="h4k-fade-in flex min-h-0 flex-1 flex-col">{children}</div>
              </div>
            </main>
          </div>
        </TooltipProvider>
      </ConfirmProvider>
    </ToastProvider>
  );
}
