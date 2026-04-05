<script lang="ts">
	import { page } from '$app/state';
	import { parseListingId } from '$lib/utils/listing';
	import { apiClient, ProblemException, type PublicCarListing } from '$lib/apiClient';
	import { t } from '$lib/i18n';
	import ErrorPage from '$lib/components/ErrorPage.svelte';
	import ListingDetails from '$lib/components/ListingDetails.svelte';

	let listing = $state<PublicCarListing | null>(null);
	let error = $state<{ status: number; message: string } | null>(null);
	let loading = $state(true);

	$effect(() => {
		const listingId = parseListingId(page.params.slug ?? '');

		listing = null;
		error = null;
		loading = true;

		if (Number.isNaN(listingId)) {
			error = { status: 404, message: $t('error.notFound') };
			loading = false;
			return;
		}

		(async () => {
			try {
				listing = await apiClient.getPublicListing(listingId);
			} catch (e) {
				if (e instanceof ProblemException) {
					if (e.problem.type === '/problems/listing-not-found') {
						error = { status: 404, message: $t('error.notFound') };
					} else {
						error = { status: e.problem.status, message: e.problem.title };
					}
				} else {
					console.error('Unexpected error while fetching listing', e);
					error = { status: 500, message: $t('error.internal') };
				}
			} finally {
				loading = false;
			}
		})();
	});
</script>

<svelte:head>
	{#if listing?.title}
		<title>{listing.title}</title>
	{/if}
</svelte:head>

{#if loading}
	<div class="flex justify-center py-24">
		<div class="h-10 w-10 animate-spin rounded-full border-4 border-gray-200 border-t-gray-600"></div>
	</div>
{:else if error}
	<ErrorPage status={error.status} message={error.message} />
{:else if listing}
	<ListingDetails {listing} />
{/if}
