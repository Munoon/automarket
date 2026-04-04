<script lang="ts">
	import { page } from '$app/state';
	import { parseListingId } from '$lib/utils/listing';

	const listingId = $derived(parseListingId(page.params.slug ?? ''));
</script>

<h1>Edit listing page</h1>
<p>Listing ID: <code>{listingId}</code></p>
