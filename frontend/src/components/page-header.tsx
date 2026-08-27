export function PageHeader({
  title,
  description,
  action,
}: {
  title: string;
  description?: string;
  action?: React.ReactNode;
}) {
  return (
    <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
      <div>
        <h1 className="text-xl font-bold sm:text-2xl">{title}</h1>
        {description && <p className="mt-1 text-sm text-[var(--muted-foreground)]">{description}</p>}
      </div>
      {action && <div className="shrink-0">{action}</div>}
    </div>
  );
}

export function EmptyState({ message }: { message: string }) {
  return (
    <div className="rounded-lg border border-dashed border-[var(--border)] bg-[var(--background)] p-12 text-center text-sm text-[var(--muted-foreground)]">
      {message}
    </div>
  );
}
