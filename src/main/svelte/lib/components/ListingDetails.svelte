<script lang="ts">
  import { Badge, Button, Carousel, Controls, CarouselIndicators } from 'flowbite-svelte';
  import {
    MapPinOutline, CalendarMonthOutline, UserCircleOutline,
    PhoneOutline, CartOutline, InfoCircleOutline
  } from 'flowbite-svelte-icons';
  import GaugeIcon from '$lib/components/icons/GaugeIcon.svelte';
  import DetailRow from '$lib/components/DetailRow.svelte';
  import noImageUrl from '$lib/assets/listing_no_image.svg?url';
  import { apiClient, type PublicCarListing } from '$lib/apiClient';
  import {
    fuelTypeKey, transmissionKey, cityKey, bodyTypeKey,
    colorKey, driveTypeKey, conditionKey, brandKey
  } from '$lib/utils/listing';
  import { t, language } from '$lib/i18n';
  import { toastStore } from '$lib/stores/toastStore';
  import { authStore } from '$lib/stores/authStore';

  let { listing, preview = false }: { listing: PublicCarListing, preview?: boolean } = $props();

  // Placeholder images until the API is ready
  const images = $derived([
    { src: noImageUrl, alt: listing.title ?? 'Car photo' },
    { src: noImageUrl, alt: listing.title ?? 'Car photo' },
    { src: noImageUrl, alt: listing.title ?? 'Car photo' },
  ]);

  let phoneState = $state<'idle' | 'loading' | 'done'>('idle');
  let phoneNumber = $state<string | null>(null);

  $effect(() => {
    if (preview) {
      phoneState = 'done';
      phoneNumber = $authStore.profile?.phoneNumber ?? '';
    }
  })

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
        <p class="text-sm font-medium text-accent uppercase tracking-wide mb-1">
          {[brandLabel, listing.model].filter(Boolean).join(' ')}
        </p>
      {/if}
      <h1 class="text-2xl sm:text-3xl font-bold text-primary leading-tight">
        {listing.title ?? '—'}
      </h1>
      <div class="flex flex-wrap items-center gap-2 mt-2 text-sm text-muted">
        {#if listing.city}
          <span class="icon-row">
            <MapPinOutline class="w-4 h-4 shrink-0" />
            {$t(cityKey(listing.city))}
          </span>
        {/if}
        {#if listing.publishedAt > 0}
          <span class="icon-row">
            <CalendarMonthOutline class="w-4 h-4 shrink-0" />
            {$t('listing.postedAt')}: {publishedDate}
          </span>
        {/if}
      </div>
    </div>
    <div class="shrink-0 text-right">
      <p class="text-3xl font-extrabold text-accent">
        {listing.price != null
          ? listing.price.toLocaleString('uk-UA', { maximumFractionDigits: 0 }) + ' ' + $t('currency.uah')
          : '—'}
      </p>
    </div>
  </div>

  <div class="grid grid-cols-1 {preview ? '' : 'lg:grid-cols-3'} gap-6">

    <!-- Left: Gallery + Description -->
    <div class="{preview ? '' : 'lg:col-span-2'} space-y-6">

      <!-- Image carousel -->
      <div class="rounded-2xl overflow-hidden surface bg-gray-100 dark:bg-gray-800">
        <Carousel {images} class="h-80 sm:h-105!" classes={{ slide: 'object-cover w-full h-full' }}>
          {#if images.length > 1}
            <Controls />
            <CarouselIndicators />
          {/if}
        </Carousel>
      </div>

      <!-- Description -->
      <div class="info-card">
        <h2 class="card-title mb-3">
          <InfoCircleOutline class="w-5 h-5 text-accent" />
          {$t('listing.description')}
        </h2>
        {#if listing.description}
          <p class="text-body whitespace-pre-line leading-relaxed text-sm">
            {listing.description}
          </p>
        {:else}
          <p class="text-muted italic text-sm">{$t('listing.noDescription')}</p>
        {/if}
      </div>
    </div>

    <!-- Right: Seller card + Details -->
    <div class="space-y-5">

      <!-- Seller card -->
      <div class="info-card">
        <h2 class="card-title mb-4">
          <UserCircleOutline class="w-5 h-5 text-accent" />
          {$t('listing.seller')}
        </h2>

        <div class="flex items-center gap-3 mb-4">
          <span class="font-medium text-body">
            {listing.authorDisplayName ?? '—'}
          </span>
        </div>

        {#if phoneState === 'idle' || phoneState === 'loading'}
          <Button color="blue" class="w-full transition-opacity dark:bg-blue-700 dark:hover:bg-blue-800 {phoneState === 'loading' ? 'opacity-60 cursor-not-allowed' : ''}" onclick={showPhone} disabled={phoneState === 'loading'}>
            <PhoneOutline class="w-4 h-4 me-2" />
            {$t('listing.showPhone')}
          </Button>
        {:else if phoneState === 'done'}
          <a href="tel:{phoneNumber}" class="phone-link">
            <PhoneOutline class="w-4 h-4 shrink-0" />
            {phoneNumber}
          </a>
        {/if}
      </div>

      <!-- Quick badges -->
      <div class="flex flex-wrap gap-2">
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
          <Badge color="gray" class="icon-row dark:bg-gray-800!">
            <GaugeIcon class="w-3 h-3 shrink-0" />
            {listing.mileage.toLocaleString()} {$t('mileage.km')}
          </Badge>
        {/if}
      </div>

      <!-- Details grid -->
      {#if listing.year || listing.mileage || listing.fuelType || listing.transmission || listing.driveType || listing.bodyType || listing.condition || listing.color || listing.engineVolume || listing.tankVolume || listing.ownersCount || listing.licensePlate}
        <div class="info-card">
          <h2 class="card-title mb-4">
            <CartOutline class="w-5 h-5 text-accent" />
            {$t('listing.details')}
          </h2>
          <dl class="space-y-0 divide-surface">
            <DetailRow label={$t('listing.detail.year')} value={listing.year} />
            <DetailRow label={$t('listing.detail.mileage')} value={listing.mileage != null ? `${listing.mileage.toLocaleString()} ${$t('mileage.km')}` : null} />
            <DetailRow label={$t('listing.detail.fuel')} value={listing.fuelType != null ? $t(fuelTypeKey(listing.fuelType)) : null} />
            <DetailRow label={$t('listing.detail.transmission')} value={listing.transmission != null ? $t(transmissionKey(listing.transmission)) : null} />
            <DetailRow label={$t('listing.detail.drive')} value={listing.driveType != null ? $t(driveTypeKey(listing.driveType)) : null} />
            <DetailRow label={$t('listing.detail.body')} value={listing.bodyType != null ? $t(bodyTypeKey(listing.bodyType)) : null} />
            <DetailRow label={$t('listing.detail.condition')} value={listing.condition != null ? $t(conditionKey(listing.condition)) : null} />
            <DetailRow label={$t('listing.detail.color')} value={listing.color != null ? $t(colorKey(listing.color)) : null} />
            <DetailRow label={$t('listing.detail.engine')} value={listing.engineVolume != null ? `${listing.engineVolume} L` : null} />
            <DetailRow label={$t('listing.detail.tank')} value={listing.tankVolume != null ? `${listing.tankVolume} L` : null} />
            <DetailRow label={$t('listing.detail.owners')} value={listing.ownersCount} />
            <DetailRow label={$t('listing.detail.plate')} value={listing.licensePlate && listing.licensePlate.length > 0 ? listing.licensePlate : null} />
          </dl>
        </div>
      {/if}
    </div>
  </div>
</div>
