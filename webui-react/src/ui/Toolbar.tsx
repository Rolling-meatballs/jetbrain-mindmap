import * as RToolbar from '@radix-ui/react-toolbar';
import * as Tooltip from '@radix-ui/react-tooltip';
import {
  ChevronsDownUp,
  ChevronsUpDown, Download, ListPlus,
  Maximize2,
  Save, Trash2,
  ZoomIn,
  ZoomOut,
} from 'lucide-react';
import type { IMindEngine } from '../engine/types';
import {
  useCommandState,
  useCommandValue,
  useEngineVersion,
  type EngineStore,
} from '../store/engineStore';
import { ToolbarButton } from './ToolbarButton';
import { TemplateMenu } from "./TemplateMenu.tsx";
import {LevelStepper} from "./LevelStepper.tsx";
import {Priority} from "./Priority.tsx";
import {NoteEditor} from "./NoteEditor.tsx";

interface ToolbarProps {
  engine: IMindEngine;
  store: EngineStore;
  onSave: () => void;
  savedLabel: string;
}

const SEP = <RToolbar.Separator className="mx-1 h-5 w-px bg-slate-200" />;

export function Toolbar({ engine, store, onSave, savedLabel }: ToolbarProps) {
  // Reactive command state/value — re-renders on engine interactchange/zoom/etc.
  const zoomOutDisabled = useCommandState(store, engine, 'zoomout') < 0;
  const zoomInDisabled = useCommandState(store, engine, 'zoomin') < 0;
  const addChildDisabled = useCommandState(store, engine, 'appendchildnode') < 0;
  const removeDisabled = useCommandState(store, engine, 'removenode') < 0;
  const zoomValue = useCommandValue<number>(store, engine, 'zoom') ?? 100;

  // Live selection count (proves the reactive store end-to-end).
  useEngineVersion(store);
  const selectedCount = engine.getSelectedNodes().length;
  const exportPng = async () => {
    const dataUrl = await engine.exportData('png');
    const a = document.createElement('a');
    a.href = dataUrl;
    a.download = 'mindmap.png';
    a.click();
  }

  const exec = (cmd: string, ...args: unknown[]) => engine.execCommand(cmd, ...args);

  return (
    <Tooltip.Provider delayDuration={400}>
      <RToolbar.Root
        aria-label="Mindmap toolbar"
        className="flex h-11 shrink-0 items-center gap-0.5 border-b border-slate-200 bg-slate-50 px-2"
      >
        <span className="mr-1 select-none px-1 text-sm font-semibold tracking-tight text-slate-700">
         My Mindmap
        </span>
        <span className="mr-1 select-none rounded bg-slate-200 px-1.5 py-0.5 text-[11px] text-slate-600">
          React · M1
        </span>

        {SEP}

        {/* Zoom group */}
        <ToolbarButton tip="缩小" icon={ZoomOut} onClick={() => exec('zoomout')} disabled={zoomOutDisabled} />
        <RToolbar.Button asChild>
          <button
            type="button"
            onClick={() => exec('zoom', 100)}
            title="重置为 100%"
            data-testid="zoom-readout"
            className="h-8 min-w-[3.25rem] rounded-md px-1 text-xs tabular-nums text-slate-600 transition-colors hover:bg-slate-200/70 hover:text-slate-900"
          >
            {Math.round(zoomValue)}%
          </button>
        </RToolbar.Button>
        <ToolbarButton tip="放大" icon={ZoomIn} onClick={() => exec('zoomin')} disabled={zoomInDisabled} />
        <ToolbarButton tip="适应窗口" icon={Maximize2} onClick={() => exec('camera')} />

        {SEP}
        {/* insert */}
        <ToolbarButton tip={"插入"} icon={ListPlus} onClick={() => exec('appendchildnode')} disabled={addChildDisabled}/>
        <ToolbarButton tip={"删除"} icon={Trash2} onClick={() => exec('removenode')} disabled={removeDisabled}/>

        {SEP}

        {/* Expand group */}
        <ToolbarButton tip="全部展开" icon={ChevronsUpDown} onClick={() => exec('expandtolevel', 99)} />
        <ToolbarButton tip="全部收起" icon={ChevronsDownUp} onClick={() => exec('expandtolevel', 1)} />

        {SEP}

        {/*Template */}
        <TemplateMenu engine={engine} store={store}/>

        {SEP}

        {/* Level Stepper*/}
        <LevelStepper engine={engine} />
        <Priority engine={engine} store={store} />

        {SEP}

        {/* Note Editor*/}
        <NoteEditor engine={engine} store={store} />

        {/* Right side */}
        <div className="ml-auto flex items-center gap-2 pr-1">
          <span className="select-none text-xs text-slate-500">
            已选 {selectedCount} 个节点{savedLabel ? ` · 已存 ${savedLabel}` : ''}
          </span>
          <RToolbar.Button asChild>
            <button
              type="button"
              onClick={onSave}
              data-testid="save-btn"
              aria-label="Save"
              className="flex h-8 items-center gap-1.5 rounded-md bg-slate-800 px-2.5 text-xs font-medium text-white transition-colors hover:bg-slate-700 active:bg-slate-900"
            >
              <Save size={15} strokeWidth={2} />
              Save
            </button>
          </RToolbar.Button>
          <ToolbarButton tip={'导出 PNG'} icon={Download} onClick={exportPng} />
        </div>
      </RToolbar.Root>
    </Tooltip.Provider>
  );
}
