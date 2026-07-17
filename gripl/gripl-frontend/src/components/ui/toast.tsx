"use client"

import React, {createContext, useCallback, useContext, useEffect, useState} from "react";
import {AlertCircle, CheckCircle2, Info, X} from "lucide-react";
import {cn} from "@/lib/utils";

type ToastVariant = "error" | "success" | "info";

interface Toast {
    id: number;
    title: string;
    description?: string;
    variant: ToastVariant;
    duration: number;
}

interface ShowToastOptions {
    title: string;
    description?: string;
    variant?: ToastVariant;
    duration?: number;
}

interface ToastContextValue {
    showToast: (options: ShowToastOptions) => void;
    showError: (title: string, description?: string) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

export function useToast(): ToastContextValue {
    const context = useContext(ToastContext);
    if (!context) {
        throw new Error("useToast must be used within a ToastProvider");
    }
    return context;
}

let toastIdCounter = 0;

export function ToastProvider({children}: { children: React.ReactNode }) {
    const [toasts, setToasts] = useState<Toast[]>([]);

    const dismiss = useCallback((id: number) => {
        setToasts((current) => current.filter((toast) => toast.id !== id));
    }, []);

    const showToast = useCallback((options: ShowToastOptions) => {
        const id = ++toastIdCounter;
        const toast: Toast = {
            id,
            title: options.title,
            description: options.description,
            variant: options.variant ?? "info",
            // Longer default for errors so the user can read the details.
            duration: options.duration ?? (options.variant === "error" ? 8000 : 4000),
        };
        setToasts((current) => [...current, toast]);
    }, []);

    const showError = useCallback((title: string, description?: string) => {
        showToast({title, description, variant: "error"});
    }, [showToast]);

    return <ToastContext.Provider value={{showToast, showError}}>
        {children}
        <div className="fixed top-4 left-1/2 -translate-x-1/2 z-[100] flex flex-col items-center gap-2 w-full max-w-2xl px-4 pointer-events-none">
            {toasts.map((toast) => (
                <ToastItem key={toast.id} toast={toast} onDismiss={dismiss}/>
            ))}
        </div>
    </ToastContext.Provider>;
}

const variantStyles: Record<ToastVariant, string> = {
    error: "border-red-500/50 bg-red-50 text-red-900 dark:bg-red-950 dark:text-red-100",
    success: "border-green-500/50 bg-green-50 text-green-900 dark:bg-green-950 dark:text-green-100",
    info: "border-border bg-background text-foreground",
};

const variantIcons: Record<ToastVariant, React.ReactNode> = {
    error: <AlertCircle className="h-5 w-5 flex-shrink-0 text-red-600 dark:text-red-400"/>,
    success: <CheckCircle2 className="h-5 w-5 flex-shrink-0 text-green-600 dark:text-green-400"/>,
    info: <Info className="h-5 w-5 flex-shrink-0 text-muted-foreground"/>,
};

function ToastItem({toast, onDismiss}: { toast: Toast; onDismiss: (id: number) => void }) {
    const [visible, setVisible] = useState(false);

    useEffect(() => {
        // Trigger the slide-in transition on mount.
        const enter = requestAnimationFrame(() => setVisible(true));
        const exit = setTimeout(() => {
            setVisible(false);
            // Remove after the exit transition finishes.
            setTimeout(() => onDismiss(toast.id), 200);
        }, toast.duration);
        return () => {
            cancelAnimationFrame(enter);
            clearTimeout(exit);
        };
    }, [toast.id, toast.duration, onDismiss]);

    return <div
        className={cn(
            "pointer-events-auto w-full rounded-lg border shadow-lg p-4 flex items-start gap-3 transition-all duration-200",
            variantStyles[toast.variant],
            visible ? "translate-y-0 opacity-100" : "-translate-y-4 opacity-0",
        )}
        role="alert"
    >
        {variantIcons[toast.variant]}
        <div className="flex-1 min-w-0">
            <p className="text-sm font-semibold">{toast.title}</p>
            {toast.description && (
                <p className="mt-1 text-sm opacity-90 break-words whitespace-pre-wrap">{toast.description}</p>
            )}
        </div>
        <button
            onClick={() => {
                setVisible(false);
                setTimeout(() => onDismiss(toast.id), 200);
            }}
            className="flex-shrink-0 opacity-70 hover:opacity-100 transition-opacity"
            aria-label="Dismiss"
        >
            <X className="h-4 w-4"/>
        </button>
    </div>;
}
