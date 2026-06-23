import {IMindEngine} from "../engine/types";
import {type EngineStore, useCommandState} from "../store/engineStore";
import {useEffect, useState} from "react";
import * as Dialog from '@radix-ui/react-dialog';
import {Link} from "lucide-react";


export function Hyperlink({ engine, store }: { engine: IMindEngine; store: EngineStore }) {
    const [open, setOpen] = useState(false);
    const disabled = useCommandState(store, engine, 'hyperlink') < 0;   // 你 Task2 的老套路:没选对节点就灰
    const [urlText, setUrlText] = useState('');                      // ← 受控输入:textarea 的内容存这
    const [title, setTitle] = useState('')

    const save = () => engine.execCommand('hyperlink', urlText, title);

    useEffect(() => {
        if(open) {
            const v = engine.queryCommandValue('hyperlink') as {url?: string; title?: string};
            setUrlText(v?.url ?? '');
            setTitle(v?.title ?? '');
        }
    }, [open])// ← 命令名你自己核

    return (
        <Dialog.Root open={open} onOpenChange={setOpen}>
            <Dialog.Trigger asChild>
                <button type="button" disabled={disabled}
                        className="flex h-8 w-8 items-center justify-center rounded-md text-slate-600 hover:bg-slate-200/70 disabled:text-slate-300">
                    <Link size={17} />
                </button>
            </Dialog.Trigger>
            <Dialog.Portal>
                <Dialog.Overlay className="fixed inset-0 bg-black/30" />
                <Dialog.Content className="fixed left-1/2 top-1/2 w-[28rem] -translate-x-1/2 -translate-y-1/2 rounded-xl bg-white p-4 shadow-xl">
                    <Dialog.Title className="mb-3 text-sm font-semibold text-slate-800">超链接</Dialog.Title>

                    <label className="mb-1 block text-xs text-slate-500">网址</label>
                    <input
                        value={urlText}
                        onChange={(e) => setUrlText(e.target.value)}
                        placeholder="https://example.com"
                        className="h-9 w-full rounded-md border border-slate-200 px-2.5 text-sm outline-none focus:border-sky-400"
                    />

                    <label className="mb-1 mt-3 block text-xs text-slate-500">显示文本（可选）</label>
                    <input
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                        placeholder="链接的显示名，留空则显示网址"
                        className="h-9 w-full rounded-md border border-slate-200 px-2.5 text-sm outline-none focus:border-sky-400"
                    />

                    <p className="mt-2 text-[11px] text-slate-400">把网址清空再保存 = 移除链接。</p>
                    <div className="mt-4 flex justify-end gap-2">
                        <Dialog.Close asChild>
                            <button type="button" className="rounded-md px-3 py-1.5 text-xs text-slate-600 hover:bg-slate-100">取消</button>
                        </Dialog.Close>
                        <Dialog.Close asChild>
                            <button type="button" onClick={save} className="rounded-md bg-slate-800 px-3 py-1.5 text-xs text-white hover:bg-slate-700">保存</button>
                        </Dialog.Close>
                    </div>
                </Dialog.Content>
            </Dialog.Portal>
        </Dialog.Root>
    );
}