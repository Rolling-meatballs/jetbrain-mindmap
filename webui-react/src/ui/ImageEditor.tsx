import type {IMindEngine} from "../engine/types";
import {type EngineStore, useCommandState} from "../store/engineStore";
import {useEffect, useId, useState} from "react";
import * as Dialog from '@radix-ui/react-dialog';
import {Image} from "lucide-react";

// Image data returned by queryCommandValue('image').
type ImageData = {
    readonly url?: string;
    readonly title?: string;
};

function readImageData(value: unknown): ImageData {
    if (typeof value !== 'object' || value === null) {
        return {};
    }

    const url = 'url' in value ? value.url : undefined;
    const title = 'title' in value ? value.title : undefined;

    return {
        ...(typeof url === 'string' ? {url} : {}),
        ...(typeof title === 'string' ? {title} : {}),
    };
}

export function ImageEditor({ engine, store }: { engine: IMindEngine; store: EngineStore }) {
    const [open, setOpen] = useState(false);
    const urlInputId = useId();
    const titleInputId = useId();
    const disabled = useCommandState(store, engine, 'image') < 0;
    const [urlText, setUrlText] = useState('');
    const [title, setTitle] = useState('');

    const save = () => {
        const trimmedUrl = urlText.trim();
        const trimmedTitle = title.trim();

        if (trimmedUrl === '') {
            engine.execCommand('image', null, '');
            return;
        }

        engine.execCommand('image', trimmedUrl, trimmedTitle);
    };

    const remove = () => {
        engine.execCommand('image', null, '');
    };

    // Populate fields from the engine when the dialog opens.
    useEffect(() => {
        if (open) {
            const v = readImageData(engine.queryCommandValue('image'));
            setUrlText(v.url ?? '');
            setTitle(v.title ?? '');
        }
    }, [engine, open]);

    return (
        <Dialog.Root open={open} onOpenChange={setOpen}>
            <Dialog.Trigger asChild>
                <button type="button" disabled={disabled} aria-label="图片"
                        className="flex h-8 w-8 items-center justify-center rounded-md text-slate-600 hover:bg-slate-200/70 disabled:text-slate-300">
                    <Image size={17} />
                </button>
            </Dialog.Trigger>
            <Dialog.Portal>
                <Dialog.Overlay className="fixed inset-0 bg-black/30" />
                <Dialog.Content className="fixed left-1/2 top-1/2 w-[28rem] -translate-x-1/2 -translate-y-1/2 rounded-xl bg-white p-4 shadow-xl">
                    <Dialog.Title className="mb-3 text-sm font-semibold text-slate-800">图片</Dialog.Title>

                    <label htmlFor={urlInputId} className="mb-1 block text-xs text-slate-500">图片 URL</label>
                    <input
                        id={urlInputId}
                        data-testid="image-url-input"
                        value={urlText}
                        onChange={(e) => setUrlText(e.target.value)}
                        placeholder="https://example.com/photo.png"
                        className="h-9 w-full rounded-md border border-slate-200 px-2.5 text-sm outline-none focus:border-sky-400"
                    />

                    <label htmlFor={titleInputId} className="mb-1 mt-3 block text-xs text-slate-500">标题（可选）</label>
                    <input
                        id={titleInputId}
                        data-testid="image-title-input"
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                        placeholder="图片说明文字"
                        className="h-9 w-full rounded-md border border-slate-200 px-2.5 text-sm outline-none focus:border-sky-400"
                    />

                    <div className="mt-4 flex justify-end gap-2">
                        <Dialog.Close asChild>
                            <button type="button"
                                    data-testid="image-remove-btn"
                                    onClick={remove}
                                    className="rounded-md px-3 py-1.5 text-xs text-red-600 hover:bg-red-50">
                                移除图片
                            </button>
                        </Dialog.Close>
                        <Dialog.Close asChild>
                            <button type="button" className="rounded-md px-3 py-1.5 text-xs text-slate-600 hover:bg-slate-100">取消</button>
                        </Dialog.Close>
                        <Dialog.Close asChild>
                            <button type="button"
                                    data-testid="image-save-btn"
                                    onClick={save}
                                    className="rounded-md bg-slate-800 px-3 py-1.5 text-xs text-white hover:bg-slate-700">
                                保存
                            </button>
                        </Dialog.Close>
                    </div>
                </Dialog.Content>
            </Dialog.Portal>
        </Dialog.Root>
    );
}
