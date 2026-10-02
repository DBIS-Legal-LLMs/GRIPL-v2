"use client"

import React, {useEffect, useState} from "react";
import {AlertTriangle, Info, X} from "lucide-react";
import {authenticatedFetch} from "@/lib/authenticated-fetch";

const STATUS_ENDPOINT = "/api/gdpr/rag/status";
const POLL_INTERVAL_MS = 2 * 60 * 1000;
const DISMISSED_KEY = "gripl.kgBanner.dismissed";

/**
 * Persistent top banner warning that the GDPR knowledge graph is empty (e.g. after
 * a fresh Neo4j volume, or a lost/wiped one) and RAG-augmented analyses won't
 * retrieve any context until the corpus is re-ingested. Polls the status endpoint
 * rather than checking once, so it clears itself once ingestion finishes.
 *
 * Stays hidden on fetch failure (proxy/RAG service down, etc.) rather than
 * flipping to a false alarm — that failure mode surfaces elsewhere when an
 * analysis is actually run.
 */
export function KgStatusBanner() {
    const [ingested, setIngested] = useState<boolean | null>(null);
    const [dismissed, setDismissed] = useState(false);

    useEffect(() => {
        try {
            setDismissed(window.localStorage.getItem(DISMISSED_KEY) === "1");
        } catch {
            /* storage unavailable — banner just won't stay dismissed */
        }
    }, []);

    const setDismissedPersisted = (value: boolean) => {
        setDismissed(value);
        try {
            if (value) window.localStorage.setItem(DISMISSED_KEY, "1");
            else window.localStorage.removeItem(DISMISSED_KEY);
        } catch {
            /* ignore */
        }
    };

    useEffect(() => {
        let cancelled = false;

        const checkStatus = () => {
            authenticatedFetch(STATUS_ENDPOINT)
                .then(async (response) => {
                    if (!response.ok) {
                        throw new Error("RAG status check failed");
                    }
                    return (await response.json()) as { ingested: boolean };
                })
                .then((status) => {
                    if (!cancelled) {
                        setIngested(status.ingested);
                    }
                })
                .catch(() => {
                    // transient/unreachable — leave the last known state as-is
                });
        };

        checkStatus();
        const interval = setInterval(checkStatus, POLL_INTERVAL_MS);
        return () => {
            cancelled = true;
            clearInterval(interval);
        };
    }, []);

    if (ingested !== false) {
        return null;
    }

    if (dismissed) {
        return (
            <button
                type="button"
                onClick={() => setDismissedPersisted(false)}
                aria-label="Show knowledge graph warning"
                title="The GDPR knowledge graph is empty — click for details"
                className="fixed top-3 right-16 z-30 rounded-full text-yellow-500 hover:text-yellow-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-yellow-500"
            >
                <Info className="h-6 w-6"/>
            </button>
        );
    }

    return (
        <div
            role="alert"
            className="fixed top-0 left-0 z-30 w-full flex items-center justify-center gap-2 pl-4 pr-10 py-2 text-sm font-medium border-b border-amber-500/50 bg-amber-50 text-amber-900 dark:bg-amber-950 dark:text-amber-100"
        >
            <AlertTriangle className="h-4 w-4 flex-shrink-0"/>
            <span>
                The GDPR knowledge graph is empty — RAG-augmented analyses won&apos;t retrieve any context.
                Re-run ingestion (<code className="font-mono">python scripts/ingest.py</code> in the gripl-rag container) to fix this.
            </span>
            <button
                type="button"
                onClick={() => setDismissedPersisted(true)}
                aria-label="Dismiss warning"
                className="absolute right-3 top-1/2 -translate-y-1/2 rounded p-1 hover:bg-amber-500/20"
            >
                <X className="h-4 w-4"/>
            </button>
        </div>
    );
}
