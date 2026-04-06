import type { Handle } from '@sveltejs/kit';
import { building } from '$app/environment';

// testing sitekey
const HCAPTCHA_SITEKEY = '10000000-ffff-ffff-ffff-000000000001';

export const handle: Handle = async ({ event, resolve }) => {
    return resolve(event, {
        // During build, adapter-static pre-renders via SSR — skip replacement so the
        // placeholder survives into the built HTML for Spring Boot to inject at serve time.
        transformPageChunk: ({ html }) => building
            ? html
            : html.replace('%%HCAPTCHA_SITEKEY%%', HCAPTCHA_SITEKEY)
    });
};
