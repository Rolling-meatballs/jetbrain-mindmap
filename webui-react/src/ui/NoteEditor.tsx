import {IMindEngine} from "../engine/types";
import {type EngineStore, useCommandState, useCommandValue} from "../store/engineStore";
import {useEffect, useState} from "react";
import * as Dialog from '@radix-ui/react-dialog';
import {StickyNote} from "lucide-react";


export function NoteEditor({ engine, store }: { engine: IMindEngine; store: EngineStore }) {
    const [open, setOpen] = useState(false);
    const currentNote = useCommandValue<string>(store, engine, 'note') ?? '';
    const disabled = useCommandState(store, engine, 'note') < 0;   // 你 Task2 的老套路:没选对节点就灰
    const [text, setText] = useState('');                          // ← 受控输入:textarea 的内容存这

    const save = () => engine.execCommand('note', text);

    useEffect(() => {
        if(open) setText(currentNote);
    }, [open])// ← 命令名你自己核

    return (
        <Dialog.Root open={open} onOpenChange={setOpen}>
            <Dialog.Trigger asChild>
                <button type="button" disabled={disabled}
                        className="flex h-8 w-8 items-center justify-center rounded-md text-slate-600 hover:bg-slate-200/70 disabled:text-slate-300">
                    <StickyNote size={17} />
                </button>
            </Dialog.Trigger>
            <Dialog.Portal>
                <Dialog.Overlay className="fixed inset-0 bg-black/30" />
                <Dialog.Content className="fixed left-1/2 top-1/2 w-[28rem] -translate-x-1/2 -translate-y-1/2 rounded-xl bg-white p-4 shadow-xl">
                    <Dialog.Title className="mb-2 text-sm font-semibold text-slate-800">节点备注</Dialog.Title>
                    <textarea
                        value={text}                                  /* ← 内容由 state 驱动 */
                        onChange={(e) => setText(e.target.value)}     /* ← 打字就更新 state */
                        placeholder="给这个节点写点备注…"
                        className="h-32 w-full resize-none rounded-md border border-slate-200 p-2 text-sm outline-none focus:border-sky-400" />
                    <div className="mt-3 flex justify-end gap-2">
                        <Dialog.Close asChild>
                            <button type="button" className="rounded-md px-3 py-1.5 text-xs text-slate-600 hover:bg-slate-100">取消</button>
                        </Dialog.Close>
                        <Dialog.Close asChild>
                            <button type="button" onClick={save} className="rounded-md bg-slate-800 px-3 py-1.5 text-xs text-white
  hover:bg-slate-700">保存</button>
                        </Dialog.Close>
                    </div>
                </Dialog.Content>
            </Dialog.Portal>
        </Dialog.Root>
    );
}