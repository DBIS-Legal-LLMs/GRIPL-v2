// Shared by the client and server fetch wrappers: which failed responses are worth one refresh and one retry.

/** 401: the access token expired (or was revoked). 403 naming the GRIPL role: the role in the token may be
 * out of date, a refresh reads the current one from auth-service (GRIPL-v2#40). Any other 403 is final. */
export async function isRefreshableFailure(res: Response): Promise<boolean> {
    if (res.status === 401) return true;
    if (res.status !== 403) return false;
    try {
        return /GRIPL role/i.test(await res.clone().text());
    } catch {
        return false;
    }
}

/** Refresh the access token this many seconds before it expires, instead of failing a request first. */
export const REFRESH_AHEAD_SECONDS = 30;
