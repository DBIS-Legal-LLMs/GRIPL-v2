"use client"

import {useEffect, useRef, useState} from "react";
import {IN_FLIGHT_STATUSES, ProcessModelListItem} from "@/models/dto/ProcessModel";

const POLL_INTERVAL_MS = 2000;

/**
 * Keeps a list of process models fresh while any of them is QUEUED/RUNNING —
 * the sequential backend job runner updates their status asynchronously, and
 * there's no push channel, so the list polls until nothing is in flight.
 *
 * Also re-syncs immediately whenever the page becomes visible again (tab
 * refocus, or the browser restoring this page from bfcache after a back
 * navigation) — otherwise a stale snapshot from before the user navigated
 * away could sit there forever with no indication that an analysis actually
 * finished (or is still running) in the meantime.
 */
export function useProcessModelListPolling(initialModels: ProcessModelListItem[]) {
    const [models, setModels] = useState<ProcessModelListItem[]>(initialModels);
    const modelsRef = useRef(models);
    modelsRef.current = models;

    useEffect(() => {
        const fetchList = () => {
            fetch("/api/process-models")
                .then(response => {
                    if (!response.ok) throw new Error(`Failed to fetch process models: ${response.statusText}`);
                    return response.json();
                })
                .then((data: ProcessModelListItem[]) => setModels(data))
                .catch(error => console.error("Error fetching process models:", error));
        };

        // Always re-sync on mount so a page restored from bfcache/router cache
        // with stale props immediately reflects the real current state.
        fetchList();

        const hasInFlight = () => modelsRef.current.some(m => IN_FLIGHT_STATUSES.includes(m.status));
        const interval = setInterval(() => {
            if (hasInFlight()) fetchList();
        }, POLL_INTERVAL_MS);

        const handleVisibilityChange = () => {
            if (document.visibilityState === "visible") fetchList();
        };
        const handlePageShow = (event: PageTransitionEvent) => {
            if (event.persisted) fetchList();
        };

        document.addEventListener("visibilitychange", handleVisibilityChange);
        window.addEventListener("pageshow", handlePageShow);

        return () => {
            clearInterval(interval);
            document.removeEventListener("visibilitychange", handleVisibilityChange);
            window.removeEventListener("pageshow", handlePageShow);
        };
    }, []);

    return {models, setModels};
}
