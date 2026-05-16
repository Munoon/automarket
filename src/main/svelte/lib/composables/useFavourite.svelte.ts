import { authStore } from '$lib/stores/authStore';
import { apiClient } from '$lib/apiClient';
import { toastStore } from '$lib/stores/toastStore';
import { withAuth } from '$lib/composables/useAuthAction';

export function useFavourite(listingId: number, initialIsFavourite: boolean) {
	let isFavourite = $state(initialIsFavourite);
	let loading = $state(false);
	let limitReached = $state(false);

	$effect(() => {
		return authStore.subscribe((auth) => {
			if (auth.initialized && !auth.profile) {
				isFavourite = false;
			}
			limitReached = auth.limits !== null && auth.favouritesCount !== null
				&& auth.favouritesCount >= auth.limits.favouritesLimitPerUser;
		});
	});

	function toggle() {
		withAuth(async () => {
			if (loading) return;
			loading = true;
			try {
				if (isFavourite) {
					await apiClient.removeFavourite({ listingId });
					isFavourite = false;
					authStore.decrementFavouritesCount();
				} else {
					await apiClient.addFavourite({ listingId });
					isFavourite = true;
					authStore.incrementFavouritesCount();
				}
			} catch (e) {
				toastStore.addApiError(e);
			} finally {
				loading = false;
			}
		});
	}

	return {
		get isFavourite() { return isFavourite; },
		get loading() { return loading; },
		get limitReached() { return limitReached; },
		toggle
	};
}
