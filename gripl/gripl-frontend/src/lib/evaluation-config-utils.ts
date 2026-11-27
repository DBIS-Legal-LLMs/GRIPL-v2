import {AnalysisEndpoint} from "@/models/evaluation/Config";

export function cryptoRandomId(): string {
    if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
        return crypto.randomUUID();
    }
    return Math.random().toString(36).slice(2);
}

export function emptyToNull(v: string): string | null {
    const t = v.trim();
    return t === "" ? null : t;
}

export function normalize<T>(v: T | null | undefined): T | null {
    return v === undefined ? null : (v as T | null);
}

export function nextLabel(base: string, existing: string[]): string {
    let i = 2;
    let candidate = `${base} (copy)`;
    while (existing.includes(candidate)) {
        candidate = `${base} (copy ${i})`;
        i++;
    }
    return candidate;
}

export function safeInt(input: string, fallback: number): number {
    const n = parseInt(input, 10);
    return Number.isFinite(n) && n > 0 ? n : fallback;
}

export function safeIntOrNull(input: string): number | null {
    const t = input.trim();
    if (t === "") return null;
    const n = parseInt(t, 10);
    return Number.isFinite(n) ? n : null;
}

export function safeFloatOrNull(input: string): number | null {
    const t = input.trim();
    if (t === "") return null;
    const n = parseFloat(t);
    return Number.isFinite(n) ? n : null;
}

export function findPreset(endpoint: string, presets: AnalysisEndpoint[]) {
    return presets.find((p) => p.endpoint === endpoint);
}

export function pruneNulls<T>(obj: T): T {
    if (obj === null || obj === undefined) return obj as T;
    if (Array.isArray(obj)) {
        return obj
            .map((item) => pruneNulls(item as T))
            .filter((v) => v !== null && v !== undefined) as unknown as T;
    }
    if (typeof obj === "object") {
        const out: Record<string, unknown> = {};
        Object.entries(obj as Record<string, unknown>).forEach(([k, v]) => {
            const vv = pruneNulls(v as T);
            if (vv !== null && vv !== undefined) out[k] = vv;
        });
        return out as T;
    }
    return obj as T;
}