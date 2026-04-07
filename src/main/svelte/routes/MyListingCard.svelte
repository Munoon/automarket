<script lang="ts">
  import { Card, Badge } from 'flowbite-svelte';
  import type { OwnCarListingListItem, ListingStatus } from '$lib/apiClient';
  import { listingSlug, listingStatusKey } from '$lib/utils/listing';
  import { t } from '$lib/i18n';

  const STATUS_COLOR: Record<ListingStatus, 'green' | 'gray' | 'yellow'> = {
    PUBLISHED: 'green',
    DRAFT:     'gray',
    ARCHIVED:  'yellow',
  };

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
    <Badge color={STATUS_COLOR[listing.status]} rounded class="text-xs rounded-sm">
      {$t(listingStatusKey(listing.status))}
    </Badge>
  </div>
</Card>
