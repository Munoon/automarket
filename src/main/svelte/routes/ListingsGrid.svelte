<script lang="ts">
  import { onMount } from 'svelte';
  import ListingCardSkeleton from '$lib/components/ListingCardSkeleton.svelte';
  import ListingCard from '$lib/components/ListingCard.svelte';
  import { apiClient, type PublicCarListingItem, ProblemException } from '$lib/apiClient';
  import { t } from '$lib/i18n';

  const PAGE_SIZE = 21;

  let listings = $state<PublicCarListingItem[]>([]);
  let loading = $state(true);
  let loadingMore = $state(false);
  let error = $state<string | null>(null);
  let currentPage = $state(0);
  let totalElements = $state(0);
  let publishedBefore = $state<number | null>(null);
  let sentinel = $state<HTMLElement | null>(null);

  const hasMore = $derived(listings.length < totalElements);

  async function loadPage(page: number) {
    try {
      const result = await apiClient.getPublicListings({ page, size: PAGE_SIZE, publishedBefore: publishedBefore ?? undefined });
      listings = [...listings, ...result.content];
      totalElements = result.totalElements;
      currentPage = page;
    } catch (e) {
      error = e instanceof ProblemException ? e.message : 'Failed to load listings.';
    }
  }

  $effect(() => {
    document.body.style.overflow = loading ? 'hidden' : '';
    return () => { document.body.style.overflow = ''; };
  });

  $effect(() => {
    if (!sentinel) return;

    const observer = new IntersectionObserver(async (entries) => {
      if (!entries[0].isIntersecting || loadingMore || !hasMore) return;
      loadingMore = true;
      await loadPage(currentPage + 1);
      loadingMore = false;
    });

    observer.observe(sentinel);
    return () => observer.disconnect();
  });

  onMount(async () => {
    publishedBefore = Date.now();
    await loadPage(0);
    loading = false;
  });
</script>

{#if loading}
  <div class="flex flex-wrap gap-4 justify-center">
    {#each { length: 9 } as _}
      <ListingCardSkeleton />
    {/each}
  </div>
{:else if error}
  <p class="text-center text-red-500 py-16">{error}</p>
{:else if listings.length === 0}
  <p class="text-center text-gray-500 py-16">{$t('listings.empty')}</p>
{:else}
  <div class="flex flex-wrap gap-4 justify-center">
    {#each listings as listing (listing.id)}
      <ListingCard {listing} />
    {/each}
    {#if hasMore}
      <div bind:this={sentinel}>
        <ListingCardSkeleton />
      </div>
      {#each { length: Math.min(5, totalElements - listings.length) } as _}
        <ListingCardSkeleton />
      {/each}
    {/if}
  </div>
{/if}
