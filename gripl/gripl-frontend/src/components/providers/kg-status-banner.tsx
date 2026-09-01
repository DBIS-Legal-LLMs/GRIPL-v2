"use client"

import React, {useEffect, useState} from "react";
import {AlertTriangle} from "lucide-react";

const STATUS_ENDPOINT = "/api/gdpr/rag/status";
const POLL_INTERVAL_MS = 2 * 60 * 1000;

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

    useEffect(() => {
        let cancelled = false;

        const checkStatus = () => {
            fetch(STATUS_ENDPOINT)
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

    return (
        <div
            role="alert"
            className="fixed top-0 left-0 z-30 w-full flex items-center justify-center gap-2 px-4 py-2 text-sm font-medium border-b border-amber-500/50 bg-amber-50 text-amber-900 dark:bg-amber-950 dark:text-amber-100"
        >
            <AlertTriangle className="h-4 w-4 flex-shrink-0"/>
            <span>
                The GDPR knowledge graph is empty — RAG-augmented analyses won&apos;t retrieve any context.
                Re-run ingestion (<code className="font-mono">python scripts/ingest.py</code> in the gripl-rag container) to fix this.
            </span>
        </div>
    );
}
