# Design Tokens — webui-react

Extracted from existing M1 toolbar and dialog components. This is a living snapshot,
not a redesign. All values come from the shipped Tailwind classes in `src/ui/`.

## Color Palette

| Token             | Tailwind class    | Usage                                    |
|--------------------|-------------------|------------------------------------------|
| slate-50           | `bg-slate-50`     | Toolbar background                       |
| slate-200          | `bg-slate-200`    | Badge background, separators              |
| slate-200/70       | `hover:bg-slate-200/70` | Icon button hover, inline btn hover |
| slate-300          | `text-slate-300`  | Disabled icon color                      |
| slate-400          | `text-slate-400`  | Hint / caption text                      |
| slate-500          | `text-slate-500`  | Input labels, secondary text            |
| slate-600          | `text-slate-600`  | Default icon color, cancel btn text       |
| slate-700          | `hover:bg-slate-700`, `text-slate-700` | Primary btn hover, active text |
| slate-800          | `bg-slate-800`    | Primary action buttons                  |
| slate-900          | `bg-slate-900`    | Tooltip background                       |
| white              | `bg-white`        | Dialog / card background                 |
| sky-400            | `focus:border-sky-400` | Input focus ring                     |
| sky-500            | `bg-sky-500`      | Active priority indicator                 |
| sky-700            | `text-sky-700`    | Active priority text                     |
| black/30           | `bg-black/30`     | Dialog overlay                           |

## Typography

| Role       | Class                                                  |
|------------|--------------------------------------------------------|
| Badge      | `text-[11px] text-slate-600 rounded px-1.5 py-0.5`    |
| Tooltip    | `text-xs text-white rounded-md px-2 py-1`             |
| Dialog title | `text-sm font-semibold text-slate-800`              |
| Input label  | `text-xs text-slate-500 mb-1`                         |
| Hint text  | `text-[11px] text-slate-400`                           |
| Button     | `text-xs` (cancel / primary), `text-xs font-medium` (save) |

## Spacing & Sizing

| Element       | Size / class                                  |
|---------------|-----------------------------------------------|
| Toolbar height | `h-11`                                       |
| Icon button    | `h-8 w-8`                                     |
| Icon size      | `size={17} strokeWidth={2}`                  |
| Dialog width   | `w-[28rem]`                                   |
| Dialog padding | `p-4`                                         |
| Input height   | `h-9`                                         |
| Input padding  | `px-2.5`                                      |
| Button padding | `px-3 py-1.5`                                 |
| Section gap    | `gap-0.5` (priority), `gap-2` (actions)      |
| Label-to-input | `mb-1` between label and input                |
| Input-to-input | `mt-3` between stacked fields                 |
| Action row     | `mt-4` above buttons                          |

## Border & Shadow

| Element         | Class                                           |
|-----------------|-------------------------------------------------|
| Toolbar bottom  | `border-b border-slate-200`                     |
| Separator       | `h-5 w-px bg-slate-200`                        |
| Input           | `border border-slate-200 rounded-md`           |
| Dialog          | `rounded-xl shadow-xl`                         |
| Button          | `rounded-md`                                    |
| Focus ring      | `outline-none focus:border-sky-400`            |

## Component Patterns

### Toolbar icon button (trigger)
```
flex h-8 w-8 items-center justify-center rounded-md
text-slate-600 hover:bg-slate-200/70 disabled:text-slate-300
```

### Dialog (Radix)
- Root: `<Dialog.Root open={open} onOpenChange={setOpen}>`
- Trigger: `<Dialog.Trigger asChild>` wrapping the icon button
- Portal + Overlay: `fixed inset-0 bg-black/30`
- Content: `fixed left-1/2 top-1/2 w-[28rem] -translate-x-1/2 -translate-y-1/2 rounded-xl bg-white p-4 shadow-xl`
- Title: `mb-3 text-sm font-semibold text-slate-800`
- Action row: `mt-4 flex justify-end gap-2`

### Toolbar layout
- Root: `flex h-11 shrink-0 items-center gap-0.5 border-b border-slate-200 bg-slate-50 px-2`
- Groups separated by `<RToolbar.Separator className="mx-1 h-5 w-px bg-slate-200" />`
- Right-aligned items in `<div className="ml-auto flex items-center gap-2 pr-1">`

### State management
- Command disabled: `useCommandState(store, engine, 'cmd') < 0`
- Command value: `useCommandValue<T>(store, engine, 'cmd')`
- Dialog populate on open: `useEffect(() => { if(open) populateState(); }, [open])`

## Icons (lucide-react)

Toolbar uses Lucide icons at `size={17}`. Available: StickyNote, Link, Image, Trash2,
ZoomIn, ZoomOut, Maximize2, Save, Download, ListPlus, ChevronsUpDown, ChevronsDownUp, etc.
