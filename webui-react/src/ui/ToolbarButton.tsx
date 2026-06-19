import * as Toolbar from '@radix-ui/react-toolbar';
import * as Tooltip from '@radix-ui/react-tooltip';
import type { LucideIcon } from 'lucide-react';

interface ToolbarButtonProps {
  tip: string;
  icon: LucideIcon;
  onClick: () => void;
  disabled?: boolean;
  active?: boolean;
}

// A toolbar icon button with an accessible tooltip. Composes Radix Toolbar.Button
// (roving focus) + Tooltip.Trigger via asChild so both land on the one <button>.
export function ToolbarButton({ tip, icon: Icon, onClick, disabled, active }: ToolbarButtonProps) {
  return (
    <Tooltip.Root>
      <Tooltip.Trigger asChild>
        <Toolbar.Button asChild>
          <button
            type="button"
            onClick={onClick}
            disabled={disabled}
            aria-label={tip}
            data-active={active || undefined}
            className="flex h-8 w-8 items-center justify-center rounded-md text-slate-600 transition-colors hover:bg-slate-200/70 hover:text-slate-900 focus-visible:outline-2 focus-visible:outline-sky-500 disabled:pointer-events-none disabled:text-slate-300 data-[active]:bg-sky-100 data-[active]:text-sky-700"
          >
            <Icon size={17} strokeWidth={2} />
          </button>
        </Toolbar.Button>
      </Tooltip.Trigger>
      <Tooltip.Portal>
        <Tooltip.Content
          sideOffset={6}
          className="z-50 rounded-md bg-slate-900 px-2 py-1 text-xs text-white shadow-md select-none"
        >
          {tip}
          <Tooltip.Arrow className="fill-slate-900" />
        </Tooltip.Content>
      </Tooltip.Portal>
    </Tooltip.Root>
  );
}
