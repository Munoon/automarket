<script lang="ts">
  import { goto } from '$app/navigation';
  import { apiClient, type OwnCarListing, type ListingStatus } from '$lib/apiClient';
  import { toastStore } from '$lib/stores/toastStore';
  import { authStore } from '$lib/stores/authStore';
  import { t, type TranslationKey } from '$lib/i18n';
  import { Button, Modal, Spinner, Dropdown, DropdownItem, Tooltip } from 'flowbite-svelte';
  import {
    ArrowLeftOutline, TrashBinOutline, ArchiveOutline,
    CheckOutline, UploadOutline, StarOutline, ExclamationCircleOutline,
    FloppyDiskOutline, ChevronDownOutline, EyeOutline
  } from 'flowbite-svelte-icons';
  import ListingStatusBadge from '$lib/components/ListingStatusBadge.svelte';
  import { listingSlug } from '$lib/utils/listing';

  let {
    listing = $bindable(),
    dirty = $bindable(),
    hasErrors,
  }: {
    listing: OwnCarListing;
    dirty: boolean;
    hasErrors: boolean;
  } = $props();

  let busy = $state(false);
  let savedBriefly = $state(false);
  let openTooltip = $state(false);
  let openCooldownTooltip = $state(false);
  let dropdownOpen = $state(false);
  let showDeleteModal = $state(false);

  let publishCooldownEnd = $derived.by(() => {
    if (listing.status === 'PUBLISHED') return null;
    const limits = $authStore.limits;
    if (!limits || !listing.publishedAt) return null;
    const end = listing.publishedAt + limits.listingRepublishCooldownMS;
    return end > Date.now() ? end : null;
  });

  let cooldownTimeStr = $derived(
    publishCooldownEnd
      ? new Date(publishCooldownEnd).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false })
      : ''
  );

  const REQUIRED_FOR_PUBLISH: { field: keyof OwnCarListing; label: TranslationKey }[] = [
    { field: 'title', label: 'edit.field.title' },
    { field: 'description', label: 'edit.field.description' },
    { field: 'brand', label: 'edit.field.brand' },
    { field: 'model', label: 'edit.field.model' },
    { field: 'licensePlate', label: 'edit.field.licensePlate' },
    { field: 'condition', label: 'edit.field.condition' },
    { field: 'mileage', label: 'edit.field.mileage' },
    { field: 'price', label: 'edit.field.price' },
    { field: 'city', label: 'edit.field.city' },
    { field: 'color', label: 'edit.field.color' },
    { field: 'transmission', label: 'edit.field.transmission' },
    { field: 'fuelType', label: 'edit.field.fuelType' },
    { field: 'tankVolume', label: 'edit.field.tankVolume' },
    { field: 'driveType', label: 'edit.field.driveType' },
    { field: 'bodyType', label: 'edit.field.bodyType' },
    { field: 'year', label: 'edit.field.year' },
    { field: 'engineVolume', label: 'edit.field.engineVolume' },
    { field: 'ownersCount', label: 'edit.field.ownersCount' }
  ];

  function validatePublish(): boolean {
    const missing = REQUIRED_FOR_PUBLISH
      .filter(({ field }) => {
        const v = listing[field];
        return v == null || v === '';
      })
      .map(({ label }) => $t(label));
    if (listing.brand === 'CUSTOM' && !listing.customBrandName?.trim()) {
      missing.push($t('edit.field.customBrand'));
    }
    if (missing.length > 0) {
      toastStore.addError(`${$t('edit.publishValidation')}: ${missing.join(', ')}`);
      return false;
    }
    return true;
  }

  async function persist(): Promise<boolean> {
    if (hasErrors) {
      toastStore.addError($t('edit.validation.hasErrors'));
      return false;
    }
    try {
      listing = await apiClient.updateOwnListing(listing.id, {
        title: listing.title,
        description: listing.description,
        price: listing.price,
        city: listing.city,
        brand: listing.brand,
        customBrandName: listing.brand === 'CUSTOM' ? listing.customBrandName : null,
        model: listing.model,
        year: listing.year,
        condition: listing.condition,
        bodyType: listing.bodyType,
        color: listing.color,
        fuelType: listing.fuelType,
        transmission: listing.transmission,
        driveType: listing.driveType,
        engineVolume: listing.engineVolume,
        tankVolume: listing.tankVolume,
        mileage: listing.mileage,
        ownersCount: listing.ownersCount,
        licensePlate: listing.licensePlate,
      });
      dirty = false;
      return true;
    } catch (e) {
      toastStore.addApiError(e);
      return false;
    }
  }

  async function saveAndPublish() {
    if (busy) return;
    if (!validatePublish()) return;
    busy = true;
    try {
      const persisted = await persist();
      if (!persisted) return;
      if (listing.status !== 'PUBLISHED') {
        await apiClient.updateOwnListingStatus(listing.id, { status: 'PUBLISHED' });
        listing = {
          ...listing,
          status: 'PUBLISHED',
          publishedAt: Date.now()
        };
      }
      savedBriefly = true;
      setTimeout(() => { savedBriefly = false; }, 2000);
    } catch (e) {
      toastStore.addApiError(e);
    } finally {
      busy = false;
    }
  }

  async function save() {
    if (busy) return;
    dropdownOpen = false;
    busy = true;
    try {
      const persisted = await persist();
      if (persisted) {
        savedBriefly = true;
        setTimeout(() => { savedBriefly = false; }, 2000);
      }
    } finally {
      busy = false;
    }
  }

  async function updateStatus(newStatus: ListingStatus) {
    if (busy) return;
    dropdownOpen = false;
    busy = true;
    try {
      await apiClient.updateOwnListingStatus(listing.id, { status: newStatus });
      listing = {
        ...listing,
        status: newStatus,
        publishedAt: newStatus === 'PUBLISHED' ? Date.now() : listing.publishedAt
      };
    } catch (e) {
      toastStore.addApiError(e);
    } finally {
      busy = false;
    }
  }

  function handleShowDeleteModal() {
    dropdownOpen = false;
    showDeleteModal = true;
  }

  async function deleteListing() {
    if (busy) return;
    busy = true;
    try {
      await apiClient.deleteOwnListing(listing.id);
      await goto('/');
      authStore.decrementOwnListingsCount();
    } catch (e) {
      toastStore.addApiError(e);
      busy = false;
    }
  }
</script>

<!-- Sticky action bar -->
<div class="sticky top-0 z-20 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 px-4 py-2.5 shadow-sm">
  <div class="max-w-7xl mx-auto flex items-center justify-between gap-3">

    <!-- Left: back + status + title -->
    <div class="flex items-center gap-2.5 min-w-0">
      <Button color="light" size="sm" class="p-1.5! shrink-0" onclick={() => goto('/')}>
        <ArrowLeftOutline class="w-4 h-4" />
      </Button>

      <ListingStatusBadge status={listing.status} />

      <span class="text-sm font-medium text-primary truncate hidden sm:block">
        {listing.title ?? $t('edit.untitled')}
      </span>

      {#if dirty}
        <span class="text-xs text-muted hidden md:block shrink-0">
          · {$t('edit.unsavedChanges')}
        </span>
      {/if}
    </div>

    <!-- Right: action buttons -->
    <div class="flex items-center gap-2 shrink-0">
      <!-- Promote button -->
      <Button color="light" size="sm" disabled class="hidden lg:flex gap-1.5 group">
        <StarOutline class="w-4 h-4 group-hover:text-yellow-400 group-hover:fill-yellow-400" />
        <span>{$t('edit.promote')}</span>
      </Button>

      <!-- Save & Publish split button -->
      <div class="flex">
        <div
          role="presentation"
          onmouseenter={() => { if (publishCooldownEnd) openCooldownTooltip = true; }}
          onmouseleave={() => openCooldownTooltip = false}>
          <Button color="green" outline size="sm" onclick={saveAndPublish} disabled={busy || !!publishCooldownEnd}
            class="gap-1.5 rounded-r-none! {publishCooldownEnd ? 'border-gray-300! text-gray-400! hover:bg-transparent! cursor-not-allowed! dark:border-gray-600! dark:text-gray-500!' : 'border-green-400! text-green-500! hover:bg-green-50! dark:border-green-500! dark:text-green-400! dark:hover:bg-green-950!'}">
            {#if busy}
              <Spinner class="w-4 h-4" />
              <span>{$t('edit.saving')}</span>
            {:else if savedBriefly}
              <CheckOutline class="w-4 h-4" />
              <span>{$t('edit.saved')}</span>
            {:else}
              <UploadOutline class="w-4 h-4" />
              <span>{$t('edit.saveAndPublish')}</span>
            {/if}
          </Button>
        </div>
        {#if publishCooldownEnd}
          <Tooltip bind:isOpen={openCooldownTooltip} placement="bottom">
            {$t('edit.republishCooldown')} {cooldownTimeStr}
          </Tooltip>
        {/if}
        <div class="flex">
          <Button color="green" outline size="sm" disabled={busy}
            class="px-2! rounded-l-none! -ml-px border-green-400! text-green-500! hover:bg-green-50! dark:border-green-500! dark:text-green-400! dark:hover:bg-green-950!">
            <ChevronDownOutline class="w-4 h-4" />
          </Button>
          <Dropdown bind:isOpen={dropdownOpen} placement="bottom-end">
            <DropdownItem onclick={save} class="flex items-center gap-2 dark:text-white">
              <FloppyDiskOutline class="w-4 h-4" />
              {$t('edit.save')}
            </DropdownItem>
            <DropdownItem
                class="flex items-center gap-2 dark:text-white {listing.status !== 'PUBLISHED' ? 'opacity-40 cursor-not-allowed' : ''}"
                onclick={() => goto(`/${listingSlug(listing.id, listing.title)}`)}
                disabled={listing.status !== 'PUBLISHED'}
                onmouseenter={() => openTooltip = true}
                onmouseleave={() => openTooltip = false}>
              <EyeOutline class="w-4 h-4" />
              {$t('edit.open')}
            </DropdownItem>
            {#if listing.status !== 'PUBLISHED'}
              <Tooltip bind:isOpen={openTooltip} placement="left">{$t('edit.open.publishFirst')}</Tooltip>
            {/if}
            {#if listing.status === 'ARCHIVED'}
              <DropdownItem onclick={() => updateStatus('DRAFT')} class="flex items-center gap-2 dark:text-white">
                <ArchiveOutline class="w-4 h-4" />
                {$t('edit.unarchive')}
              </DropdownItem>
            {:else}
              <DropdownItem onclick={() => updateStatus('ARCHIVED')} class="flex items-center gap-2 dark:text-white">
                <ArchiveOutline class="w-4 h-4" />
                {$t('edit.archive')}
              </DropdownItem>
            {/if}
            <DropdownItem onclick={handleShowDeleteModal} class="flex items-center gap-2 text-red-600 dark:text-red-400">
              <TrashBinOutline class="w-4 h-4" />
              {$t('edit.delete')}
            </DropdownItem>
          </Dropdown>
        </div>
      </div>
    </div>
  </div>
</div>

<!-- Delete confirmation modal -->
<Modal bind:open={showDeleteModal} placement="center" size="sm" class="text-center m-auto">
  <div class="flex flex-col items-center gap-4 py-2">
    <ExclamationCircleOutline class="w-14 h-14 text-red-500" />
    <div>
      <h3 class="text-lg font-semibold text-primary mb-1">{$t('edit.deleteConfirm')}</h3>
      <p class="text-sm text-muted">{$t('edit.deleteConfirmMessage')}</p>
    </div>
    <div class="flex gap-3 w-full">
      <Button color="light" class="flex-1" onclick={() => showDeleteModal = false}>
        {$t('edit.cancel')}
      </Button>
      <Button color="red" class="flex-1" onclick={deleteListing} disabled={busy}>
        {#if busy}
          <Spinner class="w-4 h-4 me-1.5" />
        {/if}
        {$t('edit.confirmDelete')}
      </Button>
    </div>
  </div>
</Modal>
