import {IMindEngine} from "../engine/types.ts";
import {useState} from "react";
import {Minus, Plus} from "lucide-react";

const MIN = 1;
const MAX = 6;

export function LevelStepper({ engine } : {engine: IMindEngine}) {
    const [level, setLevel] = useState(2);

    const applyLevel = (next: number) => {
        const clamped = Math.max(MIN, Math.min(MAX, next));
        setLevel(clamped);
        engine.execCommand('expandtolevel', clamped);
    };

    return (
        <div className={"flex h-8 items-center gap-1 rounded-md px-1 text-xs text-slate-600"}>
            <button type="button" onClick={() => applyLevel(level - 1)} disabled={level <= MIN}
                    className={"flex h-6 w-6 items-center justify-center rounded hover:bg-slate-200/70 disabled:text-slate-300"}>
                <Minus size={14}/>
            </button>
            <span className={"min-w-[3.5rem] select-none text-center tabular-nums"}>层级 { level }</span>
            <button type="button" onClick={() => applyLevel(level + 1)} disabled={level >= MAX}
                    className={"flex h-6 w-6 items-center justify-center rounded hover:bg-slate-200/70 disabled:text-slate-300"}>
                <Plus size={14} />
            </button>
        </div>
    )
}