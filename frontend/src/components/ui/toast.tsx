"use client";

import { createContext, useCallback, useContext, useEffect, useState } from "react";
import { Check, X, AlertTriangle, Info } from "lucide-react";

type ToastKind = "success" | "error" | "info" | "warning";

interface Toast {
  id: number;
  kind: ToastKind;
  message: string;
}

interface ToastContextValue {
  show: (message: string, kind?: ToastKind) => void;
  success: (message: string) => void;
  error: (message: string) => void;
  info: (message: string) => void;
  warning: (message: string) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

/** Access the toast API. Must be used under <ToastProvider>. */
export function useToast(): ToastContextValue {
  const ctx = useContext(ToastContext);
  if (!ctx) {
    // Fail soft: never crash a page because a toast wasn't wired.
    const noop = () => {};
    return { show: noop, success: noop, error: noop, info: noop, warning: noop };
  }
  return ctx;
}

let nextId = 1;

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const remove = useCallback((id: number) => {
    setToasts((t) => t.filter((x) => x.id !== id));
  }, []);

  const show = useCallback((message: string, kind: ToastKind = "info") => {
    const id = nextId++;
    setToasts((t) => [...t, { id, kind, message }]);
  }, []);

  const api: ToastContextValue = {
    show,
    success: (m) => show(m, "success"),
    error: (m) => show(m, "error"),
    info: (m) => show(m, "info"),
    warning: (m) => show(m, "warning"),
  };

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div
        className="pointer-events-none fixed inset-x-0 bottom-0 z-[100] flex flex-col items-center gap-2 p-4 sm:items-end"
        style={{ paddingBottom: "max(1rem, env(safe-area-inset-bottom))" }}
        aria-live="polite"
        aria-atomic="true"
      >
        {toasts.map((t) => (
          <ToastItem key={t.id} toast={t} onDone={() => remove(t.id)} />
        ))}
      </div>
    </ToastContext.Provider>
  );
}

const KIND_META: Record<ToastKind, { icon: typeof Check; fg: string; bg: string; border: string }> = {
  success: { icon: Check, fg: "var(--success-fg)", bg: "var(--success-bg)", border: "var(--success-border)" },
  error: { icon: X, fg: "var(--danger-fg)", bg: "var(--danger-bg)", border: "var(--danger-border)" },
  warning: { icon: AlertTriangle, fg: "var(--warning-fg)", bg: "var(--warning-bg)", border: "var(--warning-border)" },
  info: { icon: Info, fg: "var(--info-fg)", bg: "var(--info-bg)", border: "var(--info-border)" },
};

function ToastItem({ toast, onDone }: { toast: Toast; onDone: () => void }) {
  const meta = KIND_META[toast.kind];
  const Icon = meta.icon;

  useEffect(() => {
    const t = setTimeout(onDone, toast.kind === "error" ? 6000 : 3500);
    return () => clearTimeout(t);
  }, [onDone, toast.kind]);

  return (
    <div
      role="status"
      onClick={onDone}
      className="h4k-fade-in pointer-events-auto flex w-full max-w-sm cursor-pointer items-start gap-2.5 rounded-lg border bg-[var(--background)] px-4 py-3 text-sm shadow-lg"
      style={{ borderColor: meta.border }}
    >
      <span
        className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full"
        style={{ background: meta.bg, color: meta.fg }}
      >
        <Icon className="h-3.5 w-3.5" />
      </span>
      <span className="flex-1 text-[var(--foreground)]">{toast.message}</span>
    </div>
  );
}
