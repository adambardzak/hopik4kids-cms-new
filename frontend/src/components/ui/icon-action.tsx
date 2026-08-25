"use client";

import * as React from "react";
import { Button } from "@/components/ui/button";
import { Tooltip } from "@/components/ui/tooltip";
import { cn } from "@/lib/utils";

/** Icon-only action button with an accessible tooltip label. */
export function IconAction({
  label,
  icon: Icon,
  onClick,
  href,
  variant = "ghost",
  className,
  disabled,
}: {
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  onClick?: () => void;
  href?: string;
  variant?: "ghost" | "outline" | "destructive" | "default";
  className?: string;
  disabled?: boolean;
}) {
  const content = (
    <Button
      variant={variant}
      size="icon"
      onClick={onClick}
      disabled={disabled}
      aria-label={label}
      className={cn("h-9 w-9", className)}
      asChild={!!href}
    >
      {href ? (
        <a href={href}>
          <Icon className="h-4 w-4" />
        </a>
      ) : (
        <Icon className="h-4 w-4" />
      )}
    </Button>
  );

  return <Tooltip label={label}>{content}</Tooltip>;
}
