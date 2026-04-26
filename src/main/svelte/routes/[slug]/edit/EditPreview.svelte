<script lang="ts">
  import { type OwnCarListing, type PublicCarListing } from '$lib/apiClient';
  import { authStore } from '$lib/stores/authStore';
  import { t } from '$lib/i18n';
  import ListingCard from '$lib/components/ListingCard.svelte';
  import ListingDetails from '$lib/components/ListingDetails.svelte';

  let { listing }: { listing: OwnCarListing } = $props();

  let previewTab = $state<'details' | 'card'>('details');

  const fullListing = $derived<PublicCarListing>({
    ...listing,
    imageUrls: listing.images?.map(image => image.url ?? '') ?? null,
    authorDisplayName: $authStore.profile?.displayName ?? null,
    isPromoted: listing.promotedUntil > Date.now()
  });
</script>

<div class="sticky top-16 space-y-3">
  <div class="flex items-center justify-between">
    <span class="text-xs font-semibold text-muted uppercase tracking-wide">
      {$t('edit.preview')}
    </span>
    <div class="flex rounded-lg overflow-hidden border border-gray-200 dark:border-gray-700">
      <button
        class="px-3 py-1 text-xs font-medium transition-colors border-gray-200 dark:border-gray-700
          {previewTab === 'details'
            ? 'bg-blue-600 text-white'
            : 'bg-white dark:bg-gray-800 text-muted hover:bg-gray-50 dark:hover:bg-gray-700'}"
        onclick={() => previewTab = 'details'}>
        {$t('edit.preview.details')}
      </button>
      <button
        class="px-3 py-1 text-xs font-medium transition-colors
          {previewTab === 'card'
            ? 'bg-blue-600 text-white'
            : 'bg-white dark:bg-gray-800 text-muted hover:bg-gray-50 dark:hover:bg-gray-700'}"
        onclick={() => previewTab = 'card'}>
        {$t('edit.preview.card')}
      </button>
    </div>
  </div>

  {#if previewTab === 'card'}
    <div class="flex justify-center py-4">
      <ListingCard listing={fullListing} preview />
    </div>
  {:else}
    <div class="rounded-2xl border border-gray-200 dark:border-gray-700">
      <ListingDetails listing={fullListing} preview />
    </div>
  {/if}

  <p class="text-center text-xs text-muted">{$t('edit.previewHint')}</p>
</div>
