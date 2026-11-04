"use client"

import {useEffect, useState} from "react"
import {useRouter} from "next/navigation"
import {ConfirmationDialog} from "@/components/ui/confirmation-dialog"
import {registerLeaveHandler} from "@/lib/unsaved-changes"

interface UnsavedChangesGuardProps {
    /** While true, leaving the page asks for confirmation first */
    when: boolean
}

/**
 * Warns before unsaved editor changes are lost: the browser's own prompt for
 * reloads, closing the tab and external links, and a dialog for in-app links
 * and for actions routed through `confirmLeave` (e.g. logout).
 * Browser back/forward within the app is not intercepted — Next.js offers no
 * reliable hook for it.
 */
export default function UnsavedChangesGuard({when}: UnsavedChangesGuardProps) {
    const router = useRouter()
    // Wrapped in an object: a bare function passed to setState would be called as an updater.
    const [pendingLeave, setPendingLeave] = useState<{ proceed: () => void } | null>(null)

    useEffect(() => {
        if (!when) return

        const handleBeforeUnload = (event: BeforeUnloadEvent) => {
            event.preventDefault()
            event.returnValue = "" // required by older browsers to show the prompt
        }

        // Client-side navigations (Next.js <Link>) never fire beforeunload, so
        // in-app link clicks are caught in the capture phase, before Next sees them.
        const handleClick = (event: MouseEvent) => {
            if (event.defaultPrevented || event.button !== 0) return
            if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return // opens a new tab
            const anchor = (event.target as Element | null)?.closest?.("a[href]") as HTMLAnchorElement | null
            if (!anchor || anchor.target === "_blank" || anchor.hasAttribute("download")) return
            const url = new URL(anchor.href, window.location.href)
            if (url.origin !== window.location.origin) return // full page load, beforeunload handles it
            if (url.pathname === window.location.pathname && url.search === window.location.search) return
            event.preventDefault()
            event.stopPropagation()
            const href = url.pathname + url.search + url.hash
            setPendingLeave({proceed: () => router.push(href)})
        }

        // Deferred so a dropdown menu triggering it (logout) has closed before the dialog
        // opens — otherwise Radix can leave the page unclickable (pointer-events: none).
        const unregister = registerLeaveHandler(proceed => {
            window.setTimeout(() => setPendingLeave({proceed}), 0)
        })

        window.addEventListener("beforeunload", handleBeforeUnload)
        document.addEventListener("click", handleClick, true)
        return () => {
            unregister()
            window.removeEventListener("beforeunload", handleBeforeUnload)
            document.removeEventListener("click", handleClick, true)
        }
    }, [when, router])

    function handleLeave() {
        const leave = pendingLeave
        setPendingLeave(null)
        leave?.proceed()
    }

    return <ConfirmationDialog
        isOpen={pendingLeave !== null}
        onClose={() => setPendingLeave(null)}
        onConfirm={handleLeave}
        title="Discard unsaved changes?"
        description="You have changes that haven't been saved yet. If you leave this page, they will be lost."
        confirmLabel="Leave without saving"
        cancelLabel="Stay"
    />
}
