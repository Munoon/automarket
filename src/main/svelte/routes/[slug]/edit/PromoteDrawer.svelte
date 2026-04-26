<script lang="ts">
  import { apiClient, type OwnCarListing } from '$lib/apiClient';
  import { toastStore } from '$lib/stores/toastStore';
  import { t, type TranslationKey } from '$lib/i18n';
  import { Drawer, Button, Spinner, Label, Input } from 'flowbite-svelte';
  import { StarSolid } from 'flowbite-svelte-icons';

  let {
    listing = $bindable(),
    open = $bindable(false),
  }: {
    listing: OwnCarListing;
    open: boolean;
  } = $props();

  type Period = 'ONE_WEEK' | 'TWO_WEEKS' | 'ONE_MONTH';

  const PERIODS: { period: Period; labelKey: TranslationKey; price: number }[] = [
    { period: 'ONE_WEEK', labelKey: 'edit.promote.period.oneWeek', price: 199 },
    { period: 'TWO_WEEKS', labelKey: 'edit.promote.period.twoWeeks', price: 349 },
    { period: 'ONE_MONTH', labelKey: 'edit.promote.period.oneMonth', price: 599 },
  ];

  let selectedPeriod = $state<Period>('ONE_WEEK');
  let cardNumber = $state('');
  let cardExpiry = $state('');
  let cardCvv = $state('');
  let busy = $state(false);

  let selectedPrice = $derived(PERIODS.find(p => p.period === selectedPeriod)?.price ?? 0);

  $effect(() => {
    if (!open) {
      selectedPeriod = 'ONE_WEEK';
      cardNumber = '';
      cardExpiry = '';
      cardCvv = '';
    }
  });

  function handleCardNumber(e: Event) {
    const input = e.target as HTMLInputElement;
    const digits = input.value.replace(/\D/g, '').slice(0, 16);
    cardNumber = digits.replace(/(\d{4})(?=\d)/g, '$1 ');
    input.value = cardNumber;
  }

  function handleExpiry(e: Event) {
    const input = e.target as HTMLInputElement;
    const digits = input.value.replace(/\D/g, '').slice(0, 4);
    cardExpiry = digits.length > 2 ? digits.slice(0, 2) + '/' + digits.slice(2) : digits;
    input.value = cardExpiry;
  }

  function handleCvv(e: Event) {
    const input = e.target as HTMLInputElement;
    cardCvv = input.value.replace(/\D/g, '').slice(0, 3);
    input.value = cardCvv;
  }

  async function confirm() {
    if (busy) return;
    busy = true;
    try {
      listing = await apiClient.promoteOwnListing(listing.id, { period: selectedPeriod });
      open = false;
    } catch (e) {
      toastStore.addApiError(e);
    } finally {
      busy = false;
    }
  }
</script>

<Drawer bind:open placement="right" transitionParams={{ x: 400 }} class="w-full sm:w-96 flex flex-col gap-6 p-6">
  <!-- Header -->
  <div class="flex items-center gap-2">
    <StarSolid class="w-5 h-5 text-yellow-400" />
    <h5 class="text-base font-semibold text-gray-900 dark:text-white">{$t('edit.promote.title')}</h5>
  </div>

  <p class="text-sm text-gray-500 dark:text-gray-400">{$t('edit.promote.description')}</p>

  <!-- Period selection -->
  <div>
    <p class="mb-3 text-xs font-semibold uppercase tracking-wide text-gray-500 dark:text-gray-400">
      {$t('edit.promote.period.label')}
    </p>
    <div class="grid grid-cols-3 gap-2">
      {#each PERIODS as { period, labelKey, price }}
        <button
          onclick={() => selectedPeriod = period}
          class="flex cursor-pointer flex-col items-center rounded-lg border-2 p-3 text-center transition-colors
            {selectedPeriod === period
              ? 'border-yellow-400 bg-yellow-50 dark:bg-yellow-400/10!'
              : 'border-gray-200 hover:border-gray-300 dark:border-gray-600 dark:hover:border-gray-500'}">
          <span class="text-sm font-medium text-gray-900 dark:text-white">{$t(labelKey)}</span>
          <span class="mt-1 text-xs font-semibold text-yellow-600 dark:text-yellow-400">{price} {$t('currency.uah')}</span>
        </button>
      {/each}
    </div>
  </div>

  <!-- Payment details -->
  <div class="flex flex-col gap-4">
    <p class="text-xs font-semibold uppercase tracking-wide text-gray-500 dark:text-gray-400">
      {$t('edit.promote.payment.title')}
    </p>

    <Label class="flex flex-col gap-1.5">
      {$t('edit.promote.payment.card')}
      <Input
        class="font-mono"
        placeholder={$t('edit.promote.payment.cardPlaceholder')}
        value={cardNumber}
        oninput={handleCardNumber}
        maxlength={19}
        autocomplete="cc-number"
        inputmode="numeric"
      />
    </Label>

    <div class="grid grid-cols-2 gap-3">
      <Label class="flex flex-col gap-1.5">
        {$t('edit.promote.payment.expiry')}
        <Input
          class="font-mono"
          placeholder={$t('edit.promote.payment.expiryPlaceholder')}
          value={cardExpiry}
          oninput={handleExpiry}
          maxlength={5}
          autocomplete="cc-exp"
          inputmode="numeric"
        />
      </Label>
      <Label class="flex flex-col gap-1.5">
        {$t('edit.promote.payment.cvv')}
        <Input
          class="font-mono"
          placeholder={$t('edit.promote.payment.cvvPlaceholder')}
          value={cardCvv}
          oninput={handleCvv}
          maxlength={3}
          autocomplete="cc-csc"
          inputmode="numeric"
          type="password"
        />
      </Label>
    </div>
  </div>

  <!-- Total + confirm -->
  <div class="mt-auto flex flex-col gap-3">
    <div class="flex items-center justify-between text-sm">
      <span class="font-medium text-gray-600 dark:text-gray-300">{$t('edit.promote.total')}:</span>
      <span class="text-lg font-bold text-gray-900 dark:text-white">{selectedPrice} {$t('currency.uah')}</span>
    </div>
    <Button color="yellow" class="w-full gap-2" onclick={confirm} disabled={busy}>
      {#if busy}
        <Spinner class="w-4 h-4" />
        {$t('edit.promote.processing')}
      {:else}
        {$t('edit.promote.confirm')}
      {/if}
    </Button>
  </div>
</Drawer>
