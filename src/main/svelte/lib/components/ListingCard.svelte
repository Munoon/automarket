<script lang="ts">
  import { Card, Badge, Carousel, Controls } from 'flowbite-svelte';
  import { MapPinOutline } from 'flowbite-svelte-icons';
  import GaugeIcon from '$lib/components/icons/GaugeIcon.svelte';
  import noImageUrl from '$lib/assets/listing_no_image.svg?url';
  import type { PublicCarListingItem, FuelType, TransmissionType, City } from '$lib/apiClient';
  import { listingSlug, fuelTypeKey, transmissionKey, cityKey } from '$lib/utils/listing';
  import { goto } from '$app/navigation';
  import { t } from '$lib/i18n';

  let { listing }: { listing: PublicCarListingItem } = $props();

  const href = $derived(`/${listingSlug(listing.id, listing.title)}`);

  // TODO: these fields will be added to PublicCarListingItem later
  const images = $derived([{ src: noImageUrl, alt: listing.title ?? 'Car photo' }]);
  const mileage: number | null = 100;
  const fuelType: FuelType | null = 'DIESEL';
  const transmission: TransmissionType | null = 'AUTOMATIC';
  const city: City | null = 'KHARKIV';
  const year: number | null = 2025;
</script>

<Card class="w-72 p-0 overflow-hidden flex flex-col cursor-pointer hover:border-blue-500 transition-colors" onclick={() => goto(href)}>
  <div class="h-48 shrink-0 overflow-hidden">
    <Carousel {images} classes={{ slide: 'object-cover w-full h-48' }} class="h-48!">
      {#if images.length > 1}
        <div role="presentation" onclick={(e) => e.stopPropagation()}>
          <Controls />
        </div>
      {/if}
    </Carousel>
  </div>

  <div class="p-4 flex flex-col gap-2 flex-1">
    <h5 class="text-sm font-semibold text-gray-900 dark:text-white line-clamp-2 leading-snug">
      {listing.title ?? '—'}
    </h5>

    <p class="text-xl font-bold text-blue-600 dark:text-blue-400 text-right">
      {listing.price != null ? listing.price.toLocaleString('uk-UA', { maximumFractionDigits: 0 }) + ' ' + $t('currency.uah') : '—'}
    </p>

    <div class="flex gap-2 flex-wrap">
      <Badge color="gray" class="text-xs">{fuelType != null ? $t(fuelTypeKey(fuelType)) : '—'}</Badge>
      <Badge color="gray" class="text-xs">{transmission != null ? $t(transmissionKey(transmission)) : '—'}</Badge>
      <Badge color="indigo" class="text-xs">{year ?? '—'}</Badge>
    </div>

    <div class="mt-auto pt-2 border-t border-gray-100 dark:border-gray-700 flex flex-col gap-1">
      <span class="text-xs text-gray-500 dark:text-gray-400 flex items-center gap-1">
        <GaugeIcon class="w-3.5 h-3.5 shrink-0" />
        {mileage != null ? mileage.toLocaleString() + ' ' + $t('mileage.km') : '—'}
      </span>
      <span class="text-xs text-gray-500 dark:text-gray-400 flex items-center gap-1">
        <MapPinOutline class="w-3.5 h-3.5 shrink-0" />
        {city != null ? $t(cityKey(city)) : '—'}
      </span>
    </div>
  </div>
</Card>
