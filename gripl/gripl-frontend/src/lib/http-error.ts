// Walk an error payload and return the deepest human-readable message.
// Handles flat ({message}), nested ({error: {message}}), and bodies whose
// message field is *itself* a stringified JSON error (common when one service
// proxies another's error, e.g. our backend forwarding the LLM provider's body).
function digMessage(value: unknown, depth = 0): string | undefined {
    if (depth > 5) return undefined;

    if (typeof value === "string") {
        const trimmed = value.trim();
        // If the string is itself JSON, parse and keep digging.
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                return digMessage(JSON.parse(trimmed), depth + 1) ?? trimmed;
            } catch {
                return trimmed;
            }
        }
        return trimmed || undefined;
    }

    if (value && typeof value === "object") {
        const obj = value as Record<string, unknown>;
        const candidate = obj.message ?? obj.error ?? obj.detail ?? obj.title;
        if (candidate !== undefined) {
            return digMessage(candidate, depth + 1);
        }
    }

    return undefined;
}

// Try to pull a meaningful message out of a non-OK response body
// (JSON error payloads, plain text, etc.) so the user sees *why* it failed.
export async function extractErrorDetails(response: Response): Promise<string> {
    const fallback = `Server responded with ${response.status} ${response.statusText}`.trim();
    try {
        const text = await response.text();
        if (!text) return fallback;
        try {
            return digMessage(JSON.parse(text)) ?? fallback;
        } catch {
            // Not JSON — the body is already a plain message.
            return text;
        }
    } catch {
        return fallback;
    }
}

// Normalize anything thrown in a catch block into a readable string.
export function toErrorMessage(error: unknown): string {
    return error instanceof Error ? error.message : String(error);
}
