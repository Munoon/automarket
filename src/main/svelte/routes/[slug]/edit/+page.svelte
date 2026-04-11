<script lang="ts">
  import { page } from '$app/state';
  import { goto, beforeNavigate } from '$app/navigation';
  import { parseListingId } from '$lib/utils/listing';
  import { authStore } from '$lib/stores/authStore';
  import { apiClient, ProblemException, type OwnCarListing } from '$lib/apiClient';
  import { t } from '$lib/i18n';
  import ErrorPage from '$lib/components/ErrorPage.svelte';
  import ListingDetailsSkeleton from '$lib/components/ListingDetailsSkeleton.svelte';
  import EditHeader from './EditHeader.svelte';
  import EditForm from './EditForm.svelte';
  import EditPreview from './EditPreview.svelte';

  let listing = $state<OwnCarListing | null>(null);
  let loadError = $state<{ status: number; message: string } | null>(null);
  let loading = $state(true);
  let dirty = $state(false);
  let formHasErrors = $state(false);

  const listingId = $derived(parseListingId(page.params.slug ?? ''));
  const authInitialized = $derived($authStore.initialized);
  const authToken = $derived($authStore.token);

  beforeNavigate(({ cancel }) => {
    if (dirty && !confirm($t('edit.unsavedChangesLeave'))) {
      cancel();
    }
  });

  $effect(() => {
    const handler = (e: BeforeUnloadEvent) => {
      if (dirty) e.preventDefault();
    };
    window.addEventListener('beforeunload', handler);
    return () => window.removeEventListener('beforeunload', handler);
  });

  $effect(() => {
    if (!authInitialized) {
      return;
    } else if (!authToken) {
      goto('/');
      return;
    }

    listing = null;
    loadError = null;
    loading = true;
    dirty = false;

    if (Number.isNaN(listingId)) {
      loadError = { status: 404, message: $t('error.notFound') };
      loading = false;
      return;
    }

    (async () => {
      try {
        listing = await apiClient.getOwnListing(listingId);
      } catch (e) {
        if (e instanceof ProblemException) {
          loadError = { status: e.problem.status, message: e.problem.title };
        } else {
          loadError = { status: 500, message: $t('error.internal') };
        }
      } finally {
        loading = false;
      }
    })();
  });
</script>

<svelte:head>
  {#if listing}
    <title>{listing.title ?? $t('edit.untitled')} — {$t('edit.pageTitle')}</title>
  {/if}
</svelte:head>

{#if loading}
  <ListingDetailsSkeleton />
{:else if loadError}
  <ErrorPage status={loadError.status} message={loadError.message} />
{:else if listing}
  <EditHeader bind:listing bind:dirty hasErrors={formHasErrors} />
  <div class="max-w-7xl mx-auto px-4 sm:px-6 py-6">
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 items-start">
      <EditForm bind:listing bind:hasErrors={formHasErrors} onchange={() => dirty = true} />
      <EditPreview {listing} />
    </div>
  </div>
{/if}
