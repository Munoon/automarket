<script lang="ts">
  import { Card } from 'flowbite-svelte';
  import type { OwnCarListingListItem } from '$lib/apiClient';
  import { listingSlug } from '$lib/utils/listing';
  import { t } from '$lib/i18n';
  import ListingStatusBadge from '$lib/components/ListingStatusBadge.svelte';

  let { listing }: { listing: OwnCarListingListItem } = $props();
</script>

<Card
  href={`/${listingSlug(listing.id, listing.title)}/edit`}
  class="shrink-0 w-44 p-3! hover:border-blue-500 dark:hover:border-blue-400"
>
  <p class="text-xs font-semibold text-primary line-clamp-1 leading-snug mb-1">
    {listing.title ?? '—'}
  </p>
  <p class="text-sm font-bold text-accent mb-2">
    {listing.price != null ? listing.price.toLocaleString('uk-UA', { maximumFractionDigits: 0 }) + ' ' + $t('currency.uah') : '—'}
  </p>
  <div class="flex justify-end">
    <ListingStatusBadge status={listing.status} />
  </div>
</Card>
