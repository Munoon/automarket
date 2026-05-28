<script lang="ts">
  import { goto } from '$app/navigation';
  import {
    Table, TableBody, TableBodyCell, TableBodyRow, TableHead, TableHeadCell,
    Button, Spinner
  } from 'flowbite-svelte';
  import { ArrowLeftOutline, ScaleBalancedOutline } from 'flowbite-svelte-icons';
  import { authStore } from '$lib/stores/authStore';
  import { toastStore } from '$lib/stores/toastStore';
  import { apiClient, ProblemException } from '$lib/apiClient';
  import type { PublicCarListingItem } from '$lib/apiClient';
  import { t } from '$lib/i18n';
  import { fuelTypeKey, transmissionKey, cityKey, listingSlug } from '$lib/utils/listing';

  const PAGE_SIZE = 5;

  let listings = $state<PublicCarListingItem[]>([]);
  let loadingMore = $state(false);
  let totalElements = $state(0);
  let sentinel = $state<HTMLElement | null>(null);

  const hasMore = $derived(listings.length < totalElements);
  const authInitialized = $derived($authStore.initialized);
  const authToken = $derived($authStore.token);

  $effect(() => {
    if (!authInitialized || !authToken) {
      listings = [];
      totalElements = 0;
      return;
    }
    apiClient.getFavourites({ size: PAGE_SIZE }).then(result => {
      listings = result.content;
      totalElements = result.totalElements;
    }).catch(e => {
      toastStore.addError(e instanceof ProblemException ? e.message : 'Failed to load favourites.');
    });
  });

  $effect(() => {
    if (!sentinel) return;
    const observer = new IntersectionObserver(async (entries) => {
      if (!entries[0].isIntersecting || loadingMore || !hasMore) return;
      loadingMore = true;
      try {
        const result = await apiClient.getFavourites({ size: PAGE_SIZE, offset: listings.length });
        listings = [...listings, ...result.content];
        totalElements = result.totalElements;
      } catch (e) {
        toastStore.addError(e instanceof ProblemException ? e.message : 'Failed to load favourites.');
      } finally {
        loadingMore = false;
      }
    }, { threshold: 0.1 });
    observer.observe(sentinel);
    return () => observer.disconnect();
  });

  function formatPrice(price: number | null): string {
    if (price == null) return '—';
    return price.toLocaleString() + ' ' + $t('currency.uah');
  }

  function formatMileage(mileage: number | null): string {
    if (mileage == null) return '—';
    return mileage.toLocaleString() + ' ' + $t('mileage.km');
  }
</script>

<svelte:head>
  <title>{$t('favourites.comparePage')} — automarket</title>
</svelte:head>

<div class="max-w-7xl mx-auto px-6 py-8">
  <div class="flex items-center gap-3 mb-6">
    <Button color="light" size="sm" onclick={() => goto('/')}>
      <ArrowLeftOutline class="w-4 h-4 me-1" />
      {$t('favourites.title')}
    </Button>
    <h1 class="text-xl font-bold text-primary flex items-center gap-2">
      <ScaleBalancedOutline class="w-5 h-5" />
      {$t('favourites.comparePage')}
    </h1>
  </div>

  {#if !authInitialized}
    <div class="flex justify-center py-16">
      <Spinner size="8" />
    </div>
  {:else if !authToken}
    <p class="text-center text-gray-500 dark:text-gray-400 py-16">{$t('favourites.signIn')}</p>
  {:else if listings.length === 0 && !loadingMore}
    <p class="text-center text-gray-500 dark:text-gray-400 py-16">{$t('favourites.empty')}</p>
  {:else}
    <Table hoverable={true} divClass="relative overflow-x-auto rounded-lg border border-gray-200 dark:border-gray-700">
      <TableHead>
        <TableHeadCell class="sticky left-0 z-10 bg-gray-50 dark:bg-gray-700 min-w-36 w-36"></TableHeadCell>
        {#each listings as listing (listing.id)}
          <TableHeadCell class="min-w-44 p-3 font-normal">
            <a
              href="/{listingSlug(listing.id, listing.title)}"
              class="flex flex-col items-center gap-2 hover:opacity-80 transition-opacity"
            >
              {#if listing.imageUrls?.[0]}
                <img
                  src={listing.imageUrls[0]}
                  alt={listing.title ?? ''}
                  class="w-28 h-20 object-cover rounded-md"
                />
              {:else}
                <div class="w-28 h-20 bg-gray-100 dark:bg-gray-600 rounded-md flex items-center justify-center">
                  <span class="text-xs text-gray-400">—</span>
                </div>
              {/if}
              <span class="text-xs font-semibold text-gray-800 dark:text-white leading-tight text-center line-clamp-2">
                {listing.title ?? '—'}
              </span>
            </a>
          </TableHeadCell>
        {/each}
        {#if hasMore || loadingMore}
          <th bind:this={sentinel} class="min-w-20 p-4 align-middle text-center">
            {#if loadingMore}
              <Spinner size="5" />
            {/if}
          </th>
        {/if}
      </TableHead>

      <TableBody>
        <TableBodyRow>
          <TableBodyCell class="label-cell sticky left-0 z-10 bg-white dark:bg-gray-800 font-semibold whitespace-nowrap">
            {$t('listing.detail.price')}
          </TableBodyCell>
          {#each listings as listing (listing.id)}
            <TableBodyCell class="text-center font-medium">{formatPrice(listing.price)}</TableBodyCell>
          {/each}
          {#if hasMore || loadingMore}<TableBodyCell />{/if}
        </TableBodyRow>

        <TableBodyRow>
          <TableBodyCell class="label-cell sticky left-0 z-10 bg-white dark:bg-gray-800 font-semibold whitespace-nowrap">
            {$t('listing.detail.year')}
          </TableBodyCell>
          {#each listings as listing (listing.id)}
            <TableBodyCell class="text-center">{listing.year ?? '—'}</TableBodyCell>
          {/each}
          {#if hasMore || loadingMore}<TableBodyCell />{/if}
        </TableBodyRow>

        <TableBodyRow>
          <TableBodyCell class="label-cell sticky left-0 z-10 bg-white dark:bg-gray-800 font-semibold whitespace-nowrap">
            {$t('listing.detail.mileage')}
          </TableBodyCell>
          {#each listings as listing (listing.id)}
            <TableBodyCell class="text-center">{formatMileage(listing.mileage)}</TableBodyCell>
          {/each}
          {#if hasMore || loadingMore}<TableBodyCell />{/if}
        </TableBodyRow>

        <TableBodyRow>
          <TableBodyCell class="label-cell sticky left-0 z-10 bg-white dark:bg-gray-800 font-semibold whitespace-nowrap">
            {$t('listing.detail.fuel')}
          </TableBodyCell>
          {#each listings as listing (listing.id)}
            <TableBodyCell class="text-center">
              {listing.fuelType ? $t(fuelTypeKey(listing.fuelType)) : '—'}
            </TableBodyCell>
          {/each}
          {#if hasMore || loadingMore}<TableBodyCell />{/if}
        </TableBodyRow>

        <TableBodyRow>
          <TableBodyCell class="label-cell sticky left-0 z-10 bg-white dark:bg-gray-800 font-semibold whitespace-nowrap">
            {$t('listing.detail.transmission')}
          </TableBodyCell>
          {#each listings as listing (listing.id)}
            <TableBodyCell class="text-center">
              {listing.transmission ? $t(transmissionKey(listing.transmission)) : '—'}
            </TableBodyCell>
          {/each}
          {#if hasMore || loadingMore}<TableBodyCell />{/if}
        </TableBodyRow>

        <TableBodyRow>
          <TableBodyCell class="label-cell sticky left-0 z-10 bg-white dark:bg-gray-800 font-semibold whitespace-nowrap">
            {$t('listing.detail.city')}
          </TableBodyCell>
          {#each listings as listing (listing.id)}
            <TableBodyCell class="text-center">
              {listing.city ? $t(cityKey(listing.city)) : '—'}
            </TableBodyCell>
          {/each}
          {#if hasMore || loadingMore}<TableBodyCell />{/if}
        </TableBodyRow>

      </TableBody>
    </Table>
  {/if}
</div>

<style>
  /* Match Flowbite's hover colors (gray-50 / gray-600) on the sticky label cell.
     Uses a descendant selector so specificity beats flat Tailwind utilities. */
  :global(tbody tr:hover .label-cell) {
    background-color: rgb(249 250 251); /* gray-50 */
  }
  @media (prefers-color-scheme: dark) {
    :global(tbody tr:hover .label-cell) {
      background-color: rgb(75 85 99); /* gray-600 */
    }
  }
</style>
