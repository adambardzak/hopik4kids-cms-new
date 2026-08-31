"use client";

import { createContext, useCallback, useContext, useRef, useState } from "react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";

interface ConfirmOptions {
  title?: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  danger?: boolean;
}

type ConfirmFn = (options: ConfirmOptions) => Promise<boolean>;

const ConfirmContext = createContext<ConfirmFn | null>(null);

/** Access the confirm() replacement. Returns a promise<boolean>. */
export function useConfirm(): ConfirmFn {
  const ctx = useContext(ConfirmContext);
  if (!ctx) {
    // Fail soft: fall back to the native confirm if the provider isn't mounted.
    return (opts) => Promise.resolve(window.confirm(opts.message));
  }
  return ctx;
}

export function ConfirmProvider({ children }: { children: React.ReactNode }) {
  const [open, setOpen] = useState(false);
  const [opts, setOpts] = useState<ConfirmOptions>({ message: "" });
  const resolver = useRef<((v: boolean) => void) | null>(null);

  const confirm = useCallback<ConfirmFn>((options) => {
    setOpts(options);
    setOpen(true);
    return new Promise<boolean>((resolve) => {
      resolver.current = resolve;
    });
  }, []);

  function settle(result: boolean) {
    setOpen(false);
    resolver.current?.(result);
    resolver.current = null;
  }

  return (
    <ConfirmContext.Provider value={confirm}>
      {children}
      <Dialog open={open} onOpenChange={(o) => !o && settle(false)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{opts.title ?? "Potvrzení"}</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-[var(--muted-foreground)]">{opts.message}</p>
          <div className="mt-4 flex justify-end gap-2">
            <Button variant="outline" onClick={() => settle(false)}>
              {opts.cancelLabel ?? "Zrušit"}
            </Button>
            <Button
              variant={opts.danger ? "destructive" : "default"}
              onClick={() => settle(true)}
            >
              {opts.confirmLabel ?? "Potvrdit"}
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </ConfirmContext.Provider>
  );
}
