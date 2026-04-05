<script lang="ts">
  import { Badge, Button, Carousel, Controls, CarouselIndicators } from 'flowbite-svelte';
  import {
    MapPinOutline, CalendarMonthOutline, UserCircleOutline,
    PhoneOutline, CartOutline, InfoCircleOutline
  } from 'flowbite-svelte-icons';
  import GaugeIcon from '$lib/components/icons/GaugeIcon.svelte';
  import noImageUrl from '$lib/assets/listing_no_image.svg?url';
  import { apiClient, type PublicCarListing } from '$lib/apiClient';
  import {
    fuelTypeKey, transmissionKey, cityKey, bodyTypeKey,
    colorKey, driveTypeKey, conditionKey, brandKey
  } from '$lib/utils/listing';
  import { t, language } from '$lib/i18n';
  import { toastStore } from '$lib/stores/toastStore';

  let { listing }: { listing: PublicCarListing } = $props();

  // Placeholder images until the API is ready
  const images = $derived([
    { src: noImageUrl, alt: listing.title ?? 'Car photo' },
    { src: noImageUrl, alt: listing.title ?? 'Car photo' },
    { src: noImageUrl, alt: listing.title ?? 'Car photo' },
  ]);

  let phoneState = $state<'idle' | 'loading' | 'done'>('idle');
  let phoneNumber = $state<string | null>(null);

  async function showPhone() {
    if (phoneState !== 'idle') return;
    phoneState = 'loading';
    try {
      const result = await apiClient.getPublicListingAuthorPhone(listing.id);
      phoneNumber = result.phoneNumber;
      phoneState = 'done';
    } catch (e) {
      toastStore.addApiError(e);
      phoneState = 'idle';
    }
  }

  const publishedDate = $derived(
    new Date(listing.publishedAt).toLocaleDateString($language, { year: 'numeric', month: 'long', day: 'numeric' })
  );

  const brandLabel = $derived(
    listing.brand === 'CUSTOM'
      ? listing.customBrandName
      : listing.brand != null
        ? $t(brandKey(listing.brand))
        : null
  );

</script>

<div class="max-w-7xl mx-auto px-4 sm:px-6 py-8 space-y-6">

  <!-- Title + Price header -->
  <div class="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3">
    <div>
      {#if brandLabel || listing.model}
        <p class="text-sm font-medium text-blue-600 dark:text-blue-400 uppercase tracking-wide mb-1">
          {[brandLabel, listing.model].filter(Boolean).join(' ')}
        </p>
      {/if}
      <h1 class="text-2xl sm:text-3xl font-bold text-gray-900 dark:text-white leading-tight">
        {listing.title ?? '—'}
      </h1>
      <div class="flex flex-wrap items-center gap-2 mt-2 text-sm text-gray-500 dark:text-gray-400">
        {#if listing.city}
          <span class="flex items-center gap-1">
            <MapPinOutline class="w-4 h-4 shrink-0" />
            {$t(cityKey(listing.city))}
          </span>
        {/if}
        <span class="flex items-center gap-1">
          <CalendarMonthOutline class="w-4 h-4 shrink-0" />
          {$t('listing.postedAt')}: {publishedDate}
        </span>
      </div>
    </div>
    <div class="shrink-0 text-right">
      <p class="text-3xl font-extrabold text-blue-600 dark:text-blue-400">
        {listing.price != null
          ? listing.price.toLocaleString('uk-UA', { maximumFractionDigits: 0 }) + ' ' + $t('currency.uah')
          : '—'}
      </p>
    </div>
  </div>

  <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">

    <!-- Left: Gallery + Description -->
    <div class="lg:col-span-2 space-y-6">

      <!-- Image carousel -->
      <div class="rounded-2xl overflow-hidden border border-gray-200 dark:border-gray-700 bg-gray-100 dark:bg-gray-800">
        <Carousel {images} class="h-80 sm:h-[420px]!" classes={{ slide: 'object-cover w-full h-full' }}>
          {#if images.length > 1}
            <Controls />
            <CarouselIndicators />
          {/if}
        </Carousel>
      </div>

      <!-- Description -->
      <div class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 p-5">
        <h2 class="flex items-center gap-2 text-base font-semibold text-gray-900 dark:text-white mb-3">
          <InfoCircleOutline class="w-5 h-5 text-blue-500" />
          {$t('listing.description')}
        </h2>
        {#if listing.description}
          <p class="text-gray-700 dark:text-gray-300 whitespace-pre-line leading-relaxed text-sm">
            {listing.description}
          </p>
        {:else}
          <p class="text-gray-400 dark:text-gray-500 italic text-sm">{$t('listing.noDescription')}</p>
        {/if}
      </div>
    </div>

    <!-- Right: Seller card + Details -->
    <div class="space-y-5">

      <!-- Seller card -->
      <div class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 p-5">
        <h2 class="flex items-center gap-2 text-base font-semibold text-gray-900 dark:text-white mb-4">
          <UserCircleOutline class="w-5 h-5 text-blue-500" />
          {$t('listing.seller')}
        </h2>

        <div class="flex items-center gap-3 mb-4">
          <span class="font-medium text-gray-800 dark:text-gray-200">
            {listing.authorDisplayName ?? '—'}
          </span>
        </div>

        {#if phoneState === 'idle' || phoneState === 'loading'}
          <Button color="blue" class="w-full transition-opacity dark:bg-blue-700 dark:hover:bg-blue-800 {phoneState === 'loading' ? 'opacity-60 cursor-not-allowed' : ''}" onclick={showPhone} disabled={phoneState === 'loading'}>
            <PhoneOutline class="w-4 h-4 me-2" />
            {$t('listing.showPhone')}
          </Button>
        {:else if phoneState === 'done'}
          <a
            href="tel:{phoneNumber}"
            class="flex items-center justify-center gap-2 w-full rounded-lg bg-green-50 dark:bg-green-900 border border-green-200 dark:border-green-700 text-green-700 dark:text-green-400 font-semibold py-2.5 px-4 text-sm hover:bg-green-100 dark:hover:bg-green-900/50 transition-colors"
          >
            <PhoneOutline class="w-4 h-4 shrink-0" />
            {phoneNumber}
          </a>
        {/if}
      </div>

      <!-- Quick badges -->
      <div class="flex flex-wrap gap-2">
        <!-- font-medium inline-flex items-center justify-center px-2.5 py-0.5 bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-300 rounded-sm text-xs -->
        {#if listing.fuelType}
          <Badge color="gray" class="dark:bg-gray-800!">{$t(fuelTypeKey(listing.fuelType))}</Badge>
        {/if}
        {#if listing.transmission}
          <Badge color="gray" class="dark:bg-gray-800!">{$t(transmissionKey(listing.transmission))}</Badge>
        {/if}
        {#if listing.year}
          <Badge color="indigo">{listing.year}</Badge>
        {/if}
        {#if listing.condition}
          <Badge
            color={listing.condition === 'NEW' ? 'green' : 'yellow'}
            class={listing.condition === 'NEW' ? 'dark:bg-gray-800!' : ''}>
              {$t(conditionKey(listing.condition))}
            </Badge>
        {/if}
        {#if listing.mileage != null}
          <Badge color="gray" class="flex items-center gap-1 dark:bg-gray-800!">
            <GaugeIcon class="w-3 h-3 shrink-0" />
            {listing.mileage.toLocaleString()} {$t('mileage.km')}
          </Badge>
        {/if}
      </div>

      <!-- Details grid -->
      <div class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 p-5">
        <h2 class="flex items-center gap-2 text-base font-semibold text-gray-900 dark:text-white mb-4">
          <CartOutline class="w-5 h-5 text-blue-500" />
          {$t('listing.details')}
        </h2>
        <dl class="space-y-0 divide-y divide-gray-100 dark:divide-gray-700">
          {#if listing.year != null}
            <div class="flex justify-between py-2.5 text-sm">
              <dt class="text-gray-500 dark:text-gray-400">{$t('listing.detail.year')}</dt>
              <dd class="font-medium text-gray-900 dark:text-white text-right">{listing.year}</dd>
            </div>
          {/if}
          {#if listing.mileage != null}
            <div class="flex justify-between py-2.5 text-sm">
              <dt class="text-gray-500 dark:text-gray-400">{$t('listing.detail.mileage')}</dt>
              <dd class="font-medium text-gray-900 dark:text-white text-right">{listing.mileage.toLocaleString()} {$t('mileage.km')}</dd>
            </div>
          {/if}
          {#if listing.fuelType != null}
            <div class="flex justify-between py-2.5 text-sm">
              <dt class="text-gray-500 dark:text-gray-400">{$t('listing.detail.fuel')}</dt>
              <dd class="font-medium text-gray-900 dark:text-white text-right">{$t(fuelTypeKey(listing.fuelType))}</dd>
            </div>
          {/if}
          {#if listing.transmission != null}
            <div class="flex justify-between py-2.5 text-sm">
              <dt class="text-gray-500 dark:text-gray-400">{$t('listing.detail.transmission')}</dt>
              <dd class="font-medium text-gray-900 dark:text-white text-right">{$t(transmissionKey(listing.transmission))}</dd>
            </div>
          {/if}
          {#if listing.driveType != null}
            <div class="flex justify-between py-2.5 text-sm">
              <dt class="text-gray-500 dark:text-gray-400">{$t('listing.detail.drive')}</dt>
              <dd class="font-medium text-gray-900 dark:text-white text-right">{$t(driveTypeKey(listing.driveType))}</dd>
            </div>
          {/if}
          {#if listing.bodyType != null}
            <div class="flex justify-between py-2.5 text-sm">
              <dt class="text-gray-500 dark:text-gray-400">{$t('listing.detail.body')}</dt>
              <dd class="font-medium text-gray-900 dark:text-white text-right">{$t(bodyTypeKey(listing.bodyType))}</dd>
            </div>
          {/if}
          {#if listing.condition != null}
            <div class="flex justify-between py-2.5 text-sm">
              <dt class="text-gray-500 dark:text-gray-400">{$t('listing.detail.condition')}</dt>
              <dd class="font-medium text-gray-900 dark:text-white text-right">{$t(conditionKey(listing.condition))}</dd>
            </div>
          {/if}
          {#if listing.color != null}
            <div class="flex justify-between py-2.5 text-sm">
              <dt class="text-gray-500 dark:text-gray-400">{$t('listing.detail.color')}</dt>
              <dd class="font-medium text-gray-900 dark:text-white text-right">{$t(colorKey(listing.color))}</dd>
            </div>
          {/if}
          {#if listing.engineVolume != null}
            <div class="flex justify-between py-2.5 text-sm">
              <dt class="text-gray-500 dark:text-gray-400">{$t('listing.detail.engine')}</dt>
              <dd class="font-medium text-gray-900 dark:text-white text-right">{listing.engineVolume} L</dd>
            </div>
          {/if}
          {#if listing.tankVolume != null}
            <div class="flex justify-between py-2.5 text-sm">
              <dt class="text-gray-500 dark:text-gray-400">{$t('listing.detail.tank')}</dt>
              <dd class="font-medium text-gray-900 dark:text-white text-right">{listing.tankVolume} L</dd>
            </div>
          {/if}
          {#if listing.ownersCount != null}
            <div class="flex justify-between py-2.5 text-sm">
              <dt class="text-gray-500 dark:text-gray-400">{$t('listing.detail.owners')}</dt>
              <dd class="font-medium text-gray-900 dark:text-white text-right">{listing.ownersCount}</dd>
            </div>
          {/if}
          {#if listing.licensePlate != null}
            <div class="flex justify-between py-2.5 text-sm">
              <dt class="text-gray-500 dark:text-gray-400">{$t('listing.detail.plate')}</dt>
              <dd class="font-medium text-gray-900 dark:text-white text-right">{listing.licensePlate}</dd>
            </div>
          {/if}
        </dl>
      </div>

    </div>
  </div>
</div>
