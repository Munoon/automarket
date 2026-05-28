<script lang="ts">
  import { Card, Badge, Carousel, Controls, Tooltip } from 'flowbite-svelte';
  import { MapPinOutline, StarOutline, HeartOutline, HeartSolid } from 'flowbite-svelte-icons';
  import GaugeIcon from '$lib/components/icons/GaugeIcon.svelte';
  import noImageUrl from '$lib/assets/listing_no_image.svg?url';
  import type { PublicCarListingItem } from '$lib/apiClient';
  import { listingSlug, fuelTypeKey, transmissionKey, cityKey } from '$lib/utils/listing';
  import { goto } from '$app/navigation';
  import { t } from '$lib/i18n';
  import { useFavourite } from '$lib/composables/useFavourite.svelte';
  import { untrack } from 'svelte';

  let { listing, preview = false, size = 'lg' }: { listing: PublicCarListingItem, preview?: boolean, size?: 'sm' | 'lg' } = $props();

  const href = $derived(`/${listingSlug(listing.id, listing.title)}`);
  const sm = $derived(size === 'sm');
  const images = $derived.by(() => {
    if (!listing.imageUrls || listing.imageUrls.length === 0) {
      return [{ src: noImageUrl, alt: listing.title ?? 'Car photo' }];
    }

    return listing.imageUrls.map(url => ({ src: url, alt: listing.title ?? 'Car photo' }));
  });

  const favourite = useFavourite(untrack(() => listing.id), untrack(() => listing.isFavourite));

  function handleClick() {
    if (!preview) {
      goto(href);
    }
  }
</script>

<Card class="{sm ? 'w-58' : 'w-72'} shrink-0 p-0 overflow-hidden flex flex-col cursor-pointer hover:border-blue-500 transition-colors group" onclick={handleClick}>
  <div class="{sm ? 'h-42' : 'h-48'} shrink-0 overflow-hidden relative">
    <Carousel {images} classes={{ slide: `object-cover w-full ${sm ? 'h-42' : 'h-48'}` }} class={sm ? 'h-42!' : 'h-48!'}>
      {#if images.length > 1}
        <div role="presentation" onclick={(e) => e.stopPropagation()}>
          <Controls />
        </div>
      {/if}
    </Carousel>
    {#if listing.isPromoted}
      <div class="absolute top-2 right-2 z-50">
        <StarOutline id="promoted-icon-{listing.id}" class="w-5 h-5 text-yellow-400 fill-yellow-400 drop-shadow" />
        <Tooltip triggeredBy="#promoted-icon-{listing.id}">{$t('listing.promoted')}</Tooltip>
      </div>
    {/if}
    <button
      id="favourite-card-btn-{listing.id}"
      onclick={(e) => { e.stopPropagation(); favourite.toggle(); }}
      disabled={favourite.loading || preview || (favourite.limitReached && !favourite.isFavourite)}
      class="absolute top-2 left-2 z-50 p-1.5 rounded-full backdrop-blur-sm transition-all
        disabled:opacity-50 disabled:cursor-not-allowed
        {favourite.isFavourite ? 'opacity-100' : 'opacity-0 pointer-events-none group-hover:opacity-100 group-hover:pointer-events-auto'}
        {favourite.isFavourite
          ? 'bg-red-50/90 text-red-500 dark:bg-red-950/80'
          : 'bg-white/80 text-gray-400 hover:text-red-500 dark:bg-gray-900/70 dark:hover:text-red-400'}"
    >
      {#if favourite.isFavourite}
        <HeartSolid class="w-4 h-4" />
      {:else}
        <HeartOutline class="w-4 h-4" />
      {/if}
    </button>
    {#if favourite.limitReached && !favourite.isFavourite}
      <Tooltip triggeredBy="#favourite-card-btn-{listing.id}">{$t('listing.favouritesLimitReached')}</Tooltip>
    {/if}
  </div>

  <div class="p-4 flex flex-col gap-2 flex-1">
    <h5 class="text-sm font-semibold text-primary line-clamp-2 leading-snug">
      {listing.title ?? '—'}
    </h5>

    <p class="text-xl font-bold text-accent text-right">
      {listing.price != null ? listing.price.toLocaleString('uk-UA', { maximumFractionDigits: 0 }) + ' ' + $t('currency.uah') : '—'}
    </p>

    {#if !sm}
      <div class="flex gap-2 flex-wrap">
        <Badge color="gray" class="text-xs">{listing.fuelType != null ? $t(fuelTypeKey(listing.fuelType)) : '—'}</Badge>
        <Badge color="gray" class="text-xs">{listing.transmission != null ? $t(transmissionKey(listing.transmission)) : '—'}</Badge>
        <Badge color="indigo" class="text-xs">{listing.year ?? '—'}</Badge>
      </div>

      <div class="mt-auto pt-2 border-t border-gray-100 dark:border-gray-700 flex flex-col gap-1">
        <span class="text-xs text-muted icon-row">
          <GaugeIcon class="w-3.5 h-3.5 shrink-0" />
          {listing.mileage != null ? listing.mileage.toLocaleString() + ' ' + $t('mileage.km') : '—'}
        </span>
        <span class="text-xs text-muted icon-row">
          <MapPinOutline class="w-3.5 h-3.5 shrink-0" />
          {listing.city != null ? $t(cityKey(listing.city)) : '—'}
        </span>
      </div>
    {/if}
  </div>
</Card>
