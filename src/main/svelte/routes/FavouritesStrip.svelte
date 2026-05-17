<script lang="ts">
  import { Button } from 'flowbite-svelte';
  import { ChevronLeftOutline, ChevronRightOutline } from 'flowbite-svelte-icons';
  import ListingCard from '$lib/components/ListingCard.svelte';
  import ListingCardSkeleton from '$lib/components/ListingCardSkeleton.svelte';
  import { authStore } from '$lib/stores/authStore';
  import { toastStore } from '$lib/stores/toastStore';
  import { apiClient, ProblemException } from '$lib/apiClient';
  import type { PublicCarListingItem } from '$lib/apiClient';
  import { t } from '$lib/i18n';

  const PAGE_SIZE = 20;
  const CARD_WIDTH = 176 + 12; // w-44 (176px) + gap-3 (12px)

  let listings = $state<PublicCarListingItem[]>([]);
  let loadingMore = $state(false);
  let totalElements = $state(0);

  let scrollEl = $state<HTMLElement | null>(null);
  let sentinel = $state<HTMLElement | null>(null);

  const hasMore = $derived(listings.length < totalElements);

  const authInitialized = $derived($authStore.initialized);
  const authToken = $derived($authStore.token);

  $effect(() => {
    // load favourite listings only after login AND clear listings on log out
    if (!authInitialized || !authToken) {
      listings = [];
      totalElements = 0;
      return;
    }

    apiClient.getFavourites({ size: PAGE_SIZE }).then(result => {
      listings = result.content;
      totalElements = result.totalElements;
    }).catch(e => {
      toastStore.addError(e instanceof ProblemException ? e.message : 'Failed to load favourite listings.');
    });
  });

  $effect(() => {
    if (!sentinel || !scrollEl) return;

    const observer = new IntersectionObserver(async (entries) => {
      if (!entries[0].isIntersecting || loadingMore || !hasMore) return;
      loadingMore = true;
      try {
        const result = await apiClient.getFavourites({ size: PAGE_SIZE, offset: listings.length });
        listings = [...listings, ...result.content];
        totalElements = result.totalElements;
      } catch (e) {
        toastStore.addError(e instanceof ProblemException ? e.message : 'Failed to load favourite listings.');
      } finally {
        loadingMore = false;
      }
    }, { root: scrollEl });

    observer.observe(sentinel);
    return () => observer.disconnect();
  });

  function scroll(dir: -1 | 1) {
    const amount = scrollEl ? scrollEl.clientWidth - CARD_WIDTH : CARD_WIDTH * 2;
    scrollEl?.scrollBy({ left: dir * amount, behavior: 'smooth' });
  }
</script>

{#if listings.length > 0}
  <div>
    <div class="flex items-center justify-between mb-3">
      <h2 class="text-sm font-semibold text-muted">{$t('favourites.title')}</h2>
      <div class="flex gap-1">
        <Button color="light" size="xs" class="p-1!" onclick={() => scroll(-1)} aria-label="Scroll left">
          <ChevronLeftOutline class="w-4 h-4" />
        </Button>
        <Button color="light" size="xs" class="p-1!" onclick={() => scroll(1)} aria-label="Scroll right">
          <ChevronRightOutline class="w-4 h-4" />
        </Button>
      </div>
    </div>
    <div
      bind:this={scrollEl}
      class="flex gap-3 overflow-x-auto pb-2"
      style="scrollbar-width: none; -ms-overflow-style: none;"
    >
      {#each listings as listing (listing.id)}
        <ListingCard {listing} size='sm' />
      {/each}
      {#if hasMore}
        <div bind:this={sentinel} class="shrink-0 flex gap-3">
          {#each { length: 2 } as _}
            <ListingCardSkeleton size='sm' />
          {/each}
        </div>
      {/if}
    </div>
  </div>
  <hr class="my-8 border-gray-200 dark:border-gray-700" />
{/if}
