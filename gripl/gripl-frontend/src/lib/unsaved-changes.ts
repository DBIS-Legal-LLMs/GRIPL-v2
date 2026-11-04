// Lets code outside an editor (e.g. the sidebar's logout) ask before leaving
// while the editor has unsaved changes. UnsavedChangesGuard registers itself
// here while its `when` is true; only one editor is mounted at a time.

type LeaveHandler = (proceed: () => void) => void

let activeHandler: LeaveHandler | null = null

/** Registers [handler] as the one to ask before leaving; returns the unregister function. */
export function registerLeaveHandler(handler: LeaveHandler): () => void {
    activeHandler = handler
    return () => {
        if (activeHandler === handler) activeHandler = null
    }
}

/** Runs [proceed] right away, or once the user confirmed discarding unsaved editor changes. */
export function confirmLeave(proceed: () => void): void {
    if (activeHandler) {
        activeHandler(proceed)
    } else {
        proceed()
    }
}
