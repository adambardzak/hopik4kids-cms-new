import { listUsers } from "@/lib/admin-data";
import { getSession } from "@/lib/session";
import { PageHeader } from "@/components/page-header";
import { TeamManager } from "./team-manager";

export default async function TymPage() {
  const [{ items: users }, session] = await Promise.all([listUsers(), getSession()]);

  return (
    <div>
      <PageHeader
        title="Tým & role"
        description="Členové administrace. Nové členy zveš e-mailem, oni si sami nastaví heslo."
      />
      <TeamManager users={users} currentUserId={session?.userId ?? ""} />
    </div>
  );
}
