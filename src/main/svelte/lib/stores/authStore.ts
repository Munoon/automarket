import { writable } from 'svelte/store';
import type { UserProfile, Limits, AuthResponse } from '$lib/apiClient';

export interface AuthState {
	initialized: boolean;
	token: string | null;
	profile: UserProfile | null;
	limits: Limits | null;
}

const AUTH_TOKEN_STORAGE_KEY = 'automarket_auth_token';

function createAuthStore() {
	let currentState: AuthState = {
		initialized: false,
		token: null,
		profile: null,
		limits: null
	};

	const { subscribe, set } = writable<AuthState>(currentState);

	const api = {
		subscribe,
		setAuth: (authResponse: AuthResponse) => {
			const { token, profile, limits } = authResponse;
			if (typeof localStorage !== 'undefined') {
				localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, token);
			}
			currentState = {
				initialized: true,
				token,
				profile,
				limits
			};
			set(currentState);
		},
		clearAuth: () => {
			if (typeof localStorage !== 'undefined') {
				localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY);
			}
			currentState = {
				initialized: true,
				token: null,
				profile: null,
				limits: null
			};
			set(currentState);
		},
		getToken: (): string | null => {
			return currentState.token;
		},
		initialize: async () => {
			if (currentState.initialized) return;

			// Load token from localStorage
			const storedToken = typeof localStorage !== 'undefined'
				? localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)
				: null;
			if (storedToken) {
				// Update currentState first so getToken() returns the token, when fetching the profile
				currentState = {
					initialized: false,
					token: storedToken,
					profile: null,
					limits: null
				};
				set(currentState);

				// Load profile
				try {
					// Dynamically import to avoid circular dependency
					const { apiClient } = await import('$lib/apiClient');
					const profile = await apiClient.getProfile();
					
					currentState = {
						initialized: true,
						token: storedToken,
						profile,
						limits: { // TODO add actual limits here when available
							listingRepublishCooldownMS: 0,
							listingsCountLimitPerAuthor: 0
						}
					};
					set(currentState);
				} catch (error) {
					console.error('Failed to load user profile', error);
					// api.clearAuth();
				}
			} else {
				currentState = {
					initialized: true,
					token: null,
					profile: null,
					limits: null
				};
				set(currentState);
			}
		}
	};
	return api;
}

export const authStore = createAuthStore();

// Auto-initialize on module load in browser
if (typeof window !== 'undefined') {
	authStore.initialize();
}
