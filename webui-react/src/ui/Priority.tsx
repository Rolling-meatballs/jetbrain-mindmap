import {IMindEngine} from "../engine/types.ts";
import {EngineStore, useCommandState, useCommandValue} from "../store/engineStore.ts";


const LEVELS = [1, 2, 3, 4, 5, 6];

export function Priority({ engine, store }: { engine: IMindEngine; store: EngineStore }) {
    const disabled = useCommandState(store, engine, 'priority') < 0;     // 没选节点 → 整组灰(你 Task2 学的)
    const current  = useCommandValue<number>(store, engine, 'priority'); // 当前优先级(你 Task3 学的)

    return (
        <div className="flex items-center gap-0.5">
            <button type="button" disabled={disabled} onClick={() => engine.execCommand('priority', 0)}
                    className="h-6 rounded px-1.5 text-xs text-slate-500 hover:bg-slate-200/70 disabled:text-slate-300">
                清除
            </button>
            {LEVELS.map((n) => (
                <button key={n} type="button" disabled={disabled}
                        onClick={() => engine.execCommand('priority', n)}
                        data-active={current === n || undefined}
                        className="h-6 w-6 rounded text-xs tabular-nums text-slate-600 hover:bg-slate-200/70 disabled:text-slate-300 data-[active]:bg-sky-500
  data-[active]:text-white">
                    {n}
                </button>
            ))}
        </div>
    );
}