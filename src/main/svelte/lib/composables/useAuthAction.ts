import { writable, get } from 'svelte/store';
import { authStore } from '$lib/stores/authStore';

export const pendingAuthAction = writable<(() => void) | null>(null);

// Use this function from any component to execute an action requiring authentication
export function withAuth(action: () => void) {
	const auth = get(authStore);

	if (auth.profile) {
		// User is authenticated, execute immediately
		return action();
	} else if (auth.initialized) {
		// User is not authenticated, store action as pending
		pendingAuthAction.set(action);
	}
}
