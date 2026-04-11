<script lang="ts">
  import { tick } from 'svelte';
  import { Button } from 'flowbite-svelte';
  import { ChevronLeftOutline, ChevronRightOutline } from 'flowbite-svelte-icons';
  import MyListingCard from './MyListingCard.svelte';
  import { authStore } from '$lib/stores/authStore';
  import { toastStore } from '$lib/stores/toastStore';
  import { apiClient, ProblemException } from '$lib/apiClient';
  import type { OwnCarListingListItem } from '$lib/apiClient';
  import { t } from '$lib/i18n';

  const PAGE_SIZE = 20;
  const CARD_WIDTH = 176 + 12; // w-44 (176px) + gap-3 (12px)

  let listings = $state<OwnCarListingListItem[]>([]);
  let loadingMore = $state(false);
  let totalElements = $state(0);

  const visibleListings = $derived(listings.filter(l => l.status !== 'ARCHIVED'));
  const archivedListings = $derived(listings.filter(l => l.status === 'ARCHIVED'));

  let scrollEl = $state<HTMLElement | null>(null);
  let sentinel = $state<HTMLElement | null>(null);
  let archivedVisible = $state(false);

  const hasMore = $derived(listings.length < totalElements && (archivedListings.length === 0 || archivedVisible));

  const authInitialized = $derived($authStore.initialized);
  const authToken = $derived($authStore.token);

  $effect(() => {
    // load own listings only after login AND clear listings on log out
    if (!authInitialized || !authToken) {
      listings = [];
      totalElements = 0;
      archivedVisible = false;
      return;
    }

    apiClient.getOwnListings({ size: PAGE_SIZE }).then(result => {
      listings = result.content;
      totalElements = result.totalElements;
    }).catch(e => {
      toastStore.addError(e instanceof ProblemException ? e.message : 'Failed to load listings.');
    });
  });

  $effect(() => {
    if (!sentinel || !scrollEl) return;

    const observer = new IntersectionObserver(async (entries) => {
      if (!entries[0].isIntersecting || loadingMore || !hasMore) return;
      loadingMore = true;
      try {
        const result = await apiClient.getOwnListings({ size: PAGE_SIZE, offset: listings.length });
        listings = [...listings, ...result.content];
        totalElements = result.totalElements;
      } catch (e) {
        toastStore.addError(e instanceof ProblemException ? e.message : 'Failed to load listings.');
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

  async function showArchived() {
    archivedVisible = true;
    await tick();
    scroll(1);
  }
</script>

{#if listings.length > 0}
  <div>
    <div class="flex items-center justify-between mb-3">
      <h2 class="text-sm font-semibold text-muted">{$t('myListings.title')}</h2>
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
      {#each visibleListings as listing (listing.id)}
        <MyListingCard {listing} />
      {/each}
      {#if archivedListings.length > 0}
        {#if !archivedVisible}
          <div class="shrink-0 self-center">
            <Button color="light" size="sm" onclick={showArchived}>
              {$t('myListings.showArchived')} ({totalElements - visibleListings.length})
            </Button>
          </div>
        {:else}
          {#each archivedListings as listing (listing.id)}
            <MyListingCard {listing} />
          {/each}
        {/if}
      {/if}
      {#if hasMore}
        <div bind:this={sentinel} class="shrink-0 flex gap-3">
          {#each { length: 2 } as _}
            <div class="w-44 h-24 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse"></div>
          {/each}
        </div>
      {/if}
    </div>
  </div>
  <hr class="my-8 border-gray-200 dark:border-gray-700" />
{/if}
