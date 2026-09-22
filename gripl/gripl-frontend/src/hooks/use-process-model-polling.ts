"use client"

import {useEffect, useState} from "react";
import {IN_FLIGHT_STATUSES, ProcessModelDetail} from "@/models/dto/ProcessModel";

const POLL_INTERVAL_MS = 2000;

/**
 * Keeps a single process model fresh while it's QUEUED/RUNNING, so the detail
 * view picks up the analysis result as soon as the sequential job runner
 * finishes it.
 *
 * Also re-syncs immediately whenever the page becomes visible again (tab
 * refocus, or a bfcache-restored back navigation), so a stale snapshot never
 * sits there without reflecting whether the analysis is still running.
 */
export function useProcessModelPolling(initialModel: ProcessModelDetail) {
    const [model, setModel] = useState<ProcessModelDetail>(initialModel);

    useEffect(() => {
        const fetchModel = () => {
            fetch(`/api/process-models/${initialModel.id}`)
                .then(response => {
                    if (!response.ok) throw new Error(`Failed to fetch process model: ${response.statusText}`);
                    return response.json();
                })
                .then((data: ProcessModelDetail) => setModel(data))
                .catch(error => console.error("Error fetching process model:", error));
        };

        fetchModel();

        const interval = setInterval(() => {
            setModel(current => {
                if (IN_FLIGHT_STATUSES.includes(current.status)) fetchModel();
                return current;
            });
        }, POLL_INTERVAL_MS);

        const handleVisibilityChange = () => {
            if (document.visibilityState === "visible") fetchModel();
        };
        const handlePageShow = (event: PageTransitionEvent) => {
            if (event.persisted) fetchModel();
        };

        document.addEventListener("visibilitychange", handleVisibilityChange);
        window.addEventListener("pageshow", handlePageShow);

        return () => {
            clearInterval(interval);
            document.removeEventListener("visibilitychange", handleVisibilityChange);
            window.removeEventListener("pageshow", handlePageShow);
        };
    }, [initialModel.id]);

    return {model, setModel};
}
