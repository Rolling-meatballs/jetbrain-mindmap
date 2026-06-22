import * as DropdownMenu from '@radix-ui/react-dropdown-menu';
import { Check, ChevronDown, LayoutTemplate } from 'lucide-react';
import type { IMindEngine } from '../engine/types';
import { useCommandValue, type EngineStore } from '../store/engineStore';

const TEMPLATES = [
    { id: 'default', label: '思维导图' },
    { id: 'right', label: '向右展开' },
    { id: 'structure', label: '组织结构' },
    { id: 'filetree', label: '目录树' },
    { id: 'fish-bone', label: '鱼骨图' },
    { id: 'tianpan', label: '天盘图' },
];

export function TemplateMenu({ engine, store }: { engine: IMindEngine; store: EngineStore }) {
    // 读引擎当前模板(响应式:换了会自动更新标题和勾)
    const current = useCommandValue<string>(store, engine, 'template') ?? 'default';
    const currentLabel = TEMPLATES.find((t) => t.id === current)?.label ?? '思维导图';

    return (
        <DropdownMenu.Root>
            <DropdownMenu.Trigger asChild>
                <button
                    type="button"
                    className="flex h-8 items-center gap-1 rounded-md px-2 text-xs text-slate-600 transition-colors hover:bg-slate-200/70 hover:text-slate-900"
                >
                    <LayoutTemplate size={15}/>
                    {currentLabel}
                    <ChevronDown size={13} className="text-slate-400"/>
                </button>
            </DropdownMenu.Trigger>
            <DropdownMenu.Portal>
                <DropdownMenu.Content
                    sideOffset={6}
                    align={"start"}
                    className={"z-50 min-w-[9rem] rounded-lg border border-slate-200 bg-white p-1 shadow-lg"}
                >
                    {TEMPLATES.map((t) => (
                        <DropdownMenu.Item
                            key={t.id}
                            onSelect={() => engine.execCommand('template', t.id)}
                            className="flex cursor-pointer items-center justify-between rounded-md px-2 py-1.5 text-xs text-slate-700 outline-none data-[highlighted]:bg-sky-50 data-[highlighted]:text-sky-700"
                        >
                            {t.label}
                            {t.id === current && <Check size={14} className="text-sky-600" />}
                        </DropdownMenu.Item>
                    ))}
                </DropdownMenu.Content>
            </DropdownMenu.Portal>
        </DropdownMenu.Root>
    )
}