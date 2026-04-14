import { writable } from 'svelte/store';
import type { ProfileResponse, UserProfile, Limits, AuthResponse, RequestOptions } from '$lib/apiClient';
import { toastStore } from '$lib/stores/toastStore';

export interface AuthState {
	initialized: boolean;
	token: string | null;
	profile: UserProfile | null;
	limits: Limits | null;
	ownListingsCount: number | null;
}

export interface AuthStore {
	subscribe: (run: (value: AuthState) => void) => () => void;
	setAuth: (authResponse: AuthResponse) => void;
	updateProfile: (profileUpdates: Partial<UserProfile>) => void;
	clearAuth: () => void;
	getToken: () => string | null;
	initialize: (fetchProfile: (options: RequestOptions) => Promise<ProfileResponse>) => Promise<void>;
	incrementOwnListingsCount: () => void;
	decrementOwnListingsCount: () => void;
}

const AUTH_TOKEN_STORAGE_KEY = 'automarket_auth_token';

function createAuthStore(): AuthStore {
	let currentState: AuthState = {
		initialized: false,
		token: null,
		profile: null,
		limits: null,
		ownListingsCount: null
	};

	const { subscribe, set } = writable<AuthState>(currentState);

	const api = {
		subscribe,
		setAuth: (authResponse: AuthResponse) => {
			const { token, profile, limits, ownListingsCount } = authResponse;
			if (typeof localStorage !== 'undefined') {
				localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, token);
			}
			currentState = {
				initialized: true,
				token,
				profile,
				limits,
				ownListingsCount
			};
			set(currentState);
		},
		updateProfile: (profileUpdates: Partial<UserProfile>) => {
			if (!currentState.profile) return;
			currentState = {
				...currentState,
				profile: { ...currentState.profile, ...profileUpdates }
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
				limits: null,
				ownListingsCount: null
			};
			set(currentState);
		},
		getToken: (): string | null => {
			return currentState.token;
		},
		incrementOwnListingsCount: () => {
			if (currentState.ownListingsCount === null) return;
			currentState = { ...currentState, ownListingsCount: currentState.ownListingsCount + 1 };
			set(currentState);
		},
		decrementOwnListingsCount: () => {
			if (currentState.ownListingsCount === null) return;
			currentState = { ...currentState, ownListingsCount: currentState.ownListingsCount - 1 };
			set(currentState);
		},
		initialize: async (fetchProfile: (options: RequestOptions) => Promise<ProfileResponse>) => {
			if (currentState.initialized) return;

			// Load token from localStorage
			const storedToken = typeof localStorage !== 'undefined'
				? localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)
				: null;
			if (storedToken) {
				// Load profile
				try {
					const profile = await fetchProfile({ token: storedToken });
					currentState = {
						initialized: true,
						token: storedToken,
						profile: profile.user,
						limits: profile.limits,
						ownListingsCount: profile.ownListingsCount
					};
					set(currentState);
				} catch (error) {
					console.error('Failed to load user profile', error);
					toastStore.addError('Failed to load user profile');
					api.clearAuth();
				}
			} else {
				currentState = {
					initialized: true,
					token: null,
					profile: null,
					limits: null,
					ownListingsCount: null
				};
				set(currentState);
			}
		}
	};
	return api;
}

export const authStore = createAuthStore();
