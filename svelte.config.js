import adapter from '@sveltejs/adapter-static';
import { relative, sep } from 'node:path';

/** @type {import('@sveltejs/kit').Config} */
const config = {
	compilerOptions: {
		// defaults to rune mode for the project, execept for `node_modules`. Can be removed in svelte 6.
		runes: ({ filename }) => {
			const relativePath = relative(import.meta.dirname, filename);
			const pathSegments = relativePath.toLowerCase().split(sep);
			const isExternalLibrary = pathSegments.includes('node_modules');

			return isExternalLibrary ? undefined : true;
		}
	},
	kit: {
	    files: {
            assets: 'src/main/svelte/static',
            lib: 'src/main/svelte/lib',
            routes: 'src/main/svelte/routes',
            appTemplate: 'src/main/svelte/app.html'
        },
		adapter: adapter({
            pages: 'target/svelte',
            assets: 'target/svelte',
            fallback: 'fallback.html',
            precompress: false,
            strict: false
        })
	}
};

export default config;
