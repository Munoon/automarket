<script lang="ts">
	import { page } from '$app/state';
	import { parseListingId } from '$lib/utils/listing';

	const listingId = $derived(parseListingId(page.params.slug ?? ''));
</script>

<div class="px-6 py-8">
	<p>Listing #{listingId}</p>
</div>
