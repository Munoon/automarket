<script module lang="ts">
  import type { GetPublicListingsRequest } from '$lib/apiClient';
  export type Filters = Omit<GetPublicListingsRequest, 'offset' | 'size' | 'publishedBefore'>;
</script>

<script lang="ts">
  import { slide } from 'svelte/transition';
  import { Input, Button } from 'flowbite-svelte';
  import { FilterOutline, SearchOutline, ChevronDownOutline, CloseOutline } from 'flowbite-svelte-icons';
  import { t } from '$lib/i18n';
  import type { CarBrand, CarColor, CarCondition, City, DriveType, BodyType, FuelType, TransmissionType } from '$lib/apiClient';

  const BRANDS: CarBrand[] = [
    'TOYOTA', 'VOLKSWAGEN', 'BMW', 'MERCEDES_BENZ', 'AUDI', 'SKODA', 'HYUNDAI',
    'KIA', 'FORD', 'OPEL', 'RENAULT', 'PEUGEOT', 'CITROEN', 'HONDA', 'MAZDA',
    'NISSAN', 'MITSUBISHI', 'SUBARU', 'SUZUKI', 'LEXUS', 'LAND_ROVER', 'JEEP',
    'CHEVROLET', 'FIAT', 'VOLVO', 'SEAT', 'DACIA', 'ALFA_ROMEO', 'PORSCHE',
    'LADA', 'ZAZ', 'CUSTOM'
  ];
  const FUEL_TYPES: FuelType[] = ['PETROL', 'DIESEL', 'LPG', 'ELECTRIC', 'HYBRID', 'PLUG_IN_HYBRID'];
  const TRANSMISSIONS: TransmissionType[] = ['MANUAL', 'AUTOMATIC', 'CVT', 'SEMI_AUTOMATIC'];
  const CONDITIONS: CarCondition[] = ['NEW', 'USED'];
  const CITIES: City[] = [
    'KYIV', 'KHARKIV', 'ODESA', 'DNIPRO', 'ZAPORIZHZHIA', 'LVIV', 'KRYVYI_RIH',
    'MYKOLAIV', 'MARIUPOL', 'VINNYTSIA', 'KHERSON', 'POLTAVA', 'CHERNIHIV',
    'CHERKASY', 'SUMY', 'KHMELNYTSKYI', 'IVANO_FRANKIVSK', 'RIVNE', 'ZHYTOMYR',
    'TERNOPIL', 'LUTSK', 'UZHHOROD', 'CHERNIVTSI', 'KREMENCHUK', 'BILA_TSERKVA',
    'MELITOPOL', 'MUKACHEVO', 'DROHOBYCH'
  ];
  const DRIVE_TYPES: DriveType[] = ['FWD', 'RWD', 'AWD', 'FOUR_WD'];
  const BODY_TYPES: BodyType[] = [
    'SEDAN', 'HATCHBACK', 'WAGON', 'COUPE', 'CONVERTIBLE',
    'SUV', 'CROSSOVER', 'MINIVAN', 'PICKUP', 'VAN'
  ];
  const COLORS: CarColor[] = [
    'WHITE', 'BLACK', 'SILVER', 'GRAY', 'RED', 'BLUE', 'DARK_BLUE',
    'GREEN', 'DARK_GREEN', 'YELLOW', 'ORANGE', 'BROWN', 'BEIGE',
    'PURPLE', 'GOLDEN', 'BURGUNDY'
  ];
  const OWNERS_COUNTS = [1, 2, 3, 4, 5];

  let { onchange }: { onchange: (filters: Filters) => void } = $props();

  let expanded = $state(false);
  let brandOpen = $state(false);
  let cityOpen = $state(false);
  let colorOpen = $state(false);

  let query = $state('');
  let priceMin = $state('');
  let priceMax = $state('');
  let yearMin = $state('');
  let yearMax = $state('');
  let mileageMin = $state('');
  let mileageMax = $state('');
  let engineVolumeMin = $state('');
  let engineVolumeMax = $state('');
  let tankVolumeMin = $state('');
  let tankVolumeMax = $state('');
  let selectedBrands = $state<CarBrand[]>([]);
  let selectedCities = $state<City[]>([]);
  let selectedFuelTypes = $state<FuelType[]>([]);
  let selectedTransmissions = $state<TransmissionType[]>([]);
  let selectedConditions = $state<CarCondition[]>([]);
  let selectedDriveTypes = $state<DriveType[]>([]);
  let selectedBodyTypes = $state<BodyType[]>([]);
  let selectedColors = $state<CarColor[]>([]);
  let selectedOwnersCounts = $state<number[]>([]);

  let queryTimer: ReturnType<typeof setTimeout>;

  const activeFilterCount = $derived(
    (query.trim() ? 1 : 0) +
    (priceMin || priceMax ? 1 : 0) +
    (yearMin || yearMax ? 1 : 0) +
    (mileageMin || mileageMax ? 1 : 0) +
    (engineVolumeMin || engineVolumeMax ? 1 : 0) +
    (tankVolumeMin || tankVolumeMax ? 1 : 0) +
    (selectedBrands.length > 0 ? 1 : 0) +
    (selectedCities.length > 0 ? 1 : 0) +
    (selectedFuelTypes.length > 0 ? 1 : 0) +
    (selectedTransmissions.length > 0 ? 1 : 0) +
    (selectedConditions.length > 0 ? 1 : 0) +
    (selectedDriveTypes.length > 0 ? 1 : 0) +
    (selectedBodyTypes.length > 0 ? 1 : 0) +
    (selectedColors.length > 0 ? 1 : 0) +
    (selectedOwnersCounts.length > 0 ? 1 : 0)
  );

  function buildFilters(): Filters {
    return {
      query: query.trim().slice(0, 200) || undefined,
      priceMin: priceMin ? Number(priceMin) : undefined,
      priceMax: priceMax ? Number(priceMax) : undefined,
      yearMin: yearMin ? Number(yearMin) : undefined,
      yearMax: yearMax ? Number(yearMax) : undefined,
      mileageMin: mileageMin ? Number(mileageMin) : undefined,
      mileageMax: mileageMax ? Number(mileageMax) : undefined,
      engineVolumeMin: engineVolumeMin ? Number(engineVolumeMin) : undefined,
      engineVolumeMax: engineVolumeMax ? Number(engineVolumeMax) : undefined,
      tankVolumeMin: tankVolumeMin ? Number(tankVolumeMin) : undefined,
      tankVolumeMax: tankVolumeMax ? Number(tankVolumeMax) : undefined,
      brand: selectedBrands.length ? [...selectedBrands] : undefined,
      city: selectedCities.length ? [...selectedCities] : undefined,
      fuelType: selectedFuelTypes.length ? [...selectedFuelTypes] : undefined,
      transmission: selectedTransmissions.length ? [...selectedTransmissions] : undefined,
      condition: selectedConditions.length ? [...selectedConditions] : undefined,
      driveType: selectedDriveTypes.length ? [...selectedDriveTypes] : undefined,
      bodyType: selectedBodyTypes.length ? [...selectedBodyTypes] : undefined,
      color: selectedColors.length ? [...selectedColors] : undefined,
      ownersCount: selectedOwnersCounts.length ? [...selectedOwnersCounts] : undefined,
    };
  }

  function applyFilters() {
    onchange(buildFilters());
  }

  function handleQueryInput() {
    clearTimeout(queryTimer);
    queryTimer = setTimeout(applyFilters, 500);
  }

  function clearAll() {
    clearTimeout(queryTimer);
    query = '';
    priceMin = ''; priceMax = '';
    yearMin = ''; yearMax = '';
    mileageMin = ''; mileageMax = '';
    engineVolumeMin = ''; engineVolumeMax = '';
    tankVolumeMin = ''; tankVolumeMax = '';
    selectedBrands = [];
    selectedCities = [];
    selectedFuelTypes = [];
    selectedTransmissions = [];
    selectedConditions = [];
    selectedDriveTypes = [];
    selectedBodyTypes = [];
    selectedColors = [];
    selectedOwnersCounts = [];
    onchange({});
  }

  function closeAllDropdowns() {
    brandOpen = false;
    cityOpen = false;
    colorOpen = false;
  }

  function toggleValue<T>(arr: T[], value: T): T[] {
    return arr.includes(value) ? arr.filter(v => v !== value) : [...arr, value];
  }
</script>

<!-- Search + toggle row -->
<div class="flex gap-2 mb-3">
  <div class="flex-1">
    <Input
      id="carListingsFilter_query"
      class="ps-10"
      placeholder={$t('filters.searchPlaceholder')}
      bind:value={query}
      oninput={handleQueryInput}
      maxlength={200}
    >
      {#snippet left()}
        <SearchOutline class="w-4 h-4" />
      {/snippet}
    </Input>
  </div>

  {#if activeFilterCount > 0 && !expanded}
    <button
      type="button"
      class="shrink-0 text-sm text-accent hover:opacity-80 transition-opacity px-1"
      onclick={clearAll}
    >
      <CloseOutline class="w-4 h-4 inline -mt-0.5 mr-0.5" />{$t('filters.clearAll')}
    </button>
  {/if}

  <Button
    color={expanded ? 'blue' : 'light'}
    class="shrink-0 gap-1.5"
    onclick={() => { expanded = !expanded; closeAllDropdowns(); }}
  >
    <FilterOutline class="w-4 h-4" />
    {$t('filters.filters')}
    {#if activeFilterCount > 0}
      <span class="inline-flex items-center justify-center w-5 h-5 text-xs font-bold text-white bg-blue-600 rounded-full leading-none {expanded ? 'bg-white! text-blue-600!' : ''}">
        {activeFilterCount}
      </span>
    {/if}
    <ChevronDownOutline class="w-4 h-4 transition-transform duration-200 {expanded ? 'rotate-180' : ''}" />
  </Button>
</div>

{#if expanded}
  <div class="info-card mb-6 space-y-5" transition:slide={{ duration: 200 }}>

    <!-- Range filters -->
    <div class="grid grid-cols-2 lg:grid-cols-3 gap-x-4 gap-y-3">
      <div>
        <label for="carListingsFilter_priceMin" class="text-xs font-medium text-muted mb-1.5 block">{$t('filters.price')}</label>
        <div class="flex items-center gap-1.5">
          <Input id="carListingsFilter_priceMin" type="number" size="sm" placeholder={$t('filters.from')} bind:value={priceMin} onchange={applyFilters} min="0" />
          <span class="text-muted text-xs shrink-0">—</span>
          <Input id="carListingsFilter_priceMax" type="number" size="sm" placeholder={$t('filters.to')} bind:value={priceMax} onchange={applyFilters} min="0" />
        </div>
      </div>
      <div>
        <label for="carListingsFilter_yearMin" class="text-xs font-medium text-muted mb-1.5 block">{$t('edit.field.year')}</label>
        <div class="flex items-center gap-1.5">
          <Input id="carListingsFilter_yearMin" type="number" size="sm" placeholder={$t('filters.from')} bind:value={yearMin} onchange={applyFilters} min="1900" max="2030" />
          <span class="text-muted text-xs shrink-0">—</span>
          <Input id="carListingsFilter_yearMax" type="number" size="sm" placeholder={$t('filters.to')} bind:value={yearMax} onchange={applyFilters} min="1900" max="2030" />
        </div>
      </div>
      <div>
        <label for="carListingsFilter_mileageMin" class="text-xs font-medium text-muted mb-1.5 block">{$t('filters.mileage')}</label>
        <div class="flex items-center gap-1.5">
          <Input id="carListingsFilter_mileageMin" type="number" size="sm" placeholder={$t('filters.from')} bind:value={mileageMin} onchange={applyFilters} min="0" />
          <span class="text-muted text-xs shrink-0">—</span>
          <Input id="carListingsFilter_mileageMax" type="number" size="sm" placeholder={$t('filters.to')} bind:value={mileageMax} onchange={applyFilters} min="0" />
        </div>
      </div>
      <div>
        <label for="carListingsFilter_engineVolumeMin" class="text-xs font-medium text-muted mb-1.5 block">{$t('filters.engineVolume')}</label>
        <div class="flex items-center gap-1.5">
          <Input id="carListingsFilter_engineVolumeMin" type="number" size="sm" placeholder={$t('filters.from')} bind:value={engineVolumeMin} onchange={applyFilters} min="0" step="0.1" />
          <span class="text-muted text-xs shrink-0">—</span>
          <Input id="carListingsFilter_engineVolumeMax" type="number" size="sm" placeholder={$t('filters.to')} bind:value={engineVolumeMax} onchange={applyFilters} min="0" step="0.1" />
        </div>
      </div>
      <div>
        <label for="carListingsFilter_tankVolumeMin" class="text-xs font-medium text-muted mb-1.5 block">{$t('filters.tankVolume')}</label>
        <div class="flex items-center gap-1.5">
          <Input id="carListingsFilter_tankVolumeMin" type="number" size="sm" placeholder={$t('filters.from')} bind:value={tankVolumeMin} onchange={applyFilters} min="0" />
          <span class="text-muted text-xs shrink-0">—</span>
          <Input id="carListingsFilter_tankVolumeMax" type="number" size="sm" placeholder={$t('filters.to')} bind:value={tankVolumeMax} onchange={applyFilters} min="0" />
        </div>
      </div>
    </div>

    <hr class="border-gray-100 dark:border-gray-700" />

    <!-- Multi-select dropdown chips (Brand, City, Color) -->
    <div class="flex flex-wrap gap-2">
      {#snippet dropdownChip(label: string, count: number, isOpen: boolean, toggle: () => void)}
        <button
          type="button"
          onclick={toggle}
          class="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-full border transition-all cursor-pointer select-none focus:outline-none
            {count > 0
              ? 'border-blue-500 border-2 bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400 font-medium'
              : 'border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-700 text-body hover:border-gray-400 dark:hover:border-gray-400'}"
        >
          {label}
          {#if count > 0}
            <span class="inline-flex items-center justify-center w-4 h-4 text-xs font-bold text-white bg-blue-500 rounded-full leading-none">{count}</span>
          {/if}
          <ChevronDownOutline class="w-3 h-3 transition-transform duration-150 {isOpen ? 'rotate-180' : ''}" />
        </button>
      {/snippet}

      <!-- Brand -->
      <div class="relative">
        {@render dropdownChip($t('edit.field.brand'), selectedBrands.length, brandOpen, () => { brandOpen = !brandOpen; cityOpen = false; colorOpen = false; })}
        {#if brandOpen}
          <button type="button" class="fixed inset-0 z-40" onclick={() => brandOpen = false} aria-label="Close"></button>
          <div class="absolute z-50 mt-1.5 w-52 surface rounded-xl shadow-lg max-h-64 overflow-y-auto py-1">
            {#each BRANDS as brand}
              <label for="carListingsFilter_brand_{brand}" class="flex items-center gap-2.5 px-3 py-2 hover:bg-surface-hover cursor-pointer text-sm text-primary">
                <input id="carListingsFilter_brand_{brand}" type="checkbox" bind:group={selectedBrands} value={brand} onchange={applyFilters}
                  class="w-4 h-4 text-blue-600 rounded border-gray-300 dark:border-gray-500 dark:bg-gray-600 focus:ring-blue-500" />
                {$t(`brand.${brand}`)}
              </label>
            {/each}
          </div>
        {/if}
      </div>

      <!-- City -->
      <div class="relative">
        {@render dropdownChip($t('edit.field.city'), selectedCities.length, cityOpen, () => { cityOpen = !cityOpen; brandOpen = false; colorOpen = false; })}
        {#if cityOpen}
          <button type="button" class="fixed inset-0 z-40" onclick={() => cityOpen = false} aria-label="Close"></button>
          <div class="absolute z-50 mt-1.5 w-52 surface rounded-xl shadow-lg max-h-64 overflow-y-auto py-1">
            {#each CITIES as city}
              <label for="carListingsFilter_city_{city}" class="flex items-center gap-2.5 px-3 py-2 hover:bg-surface-hover cursor-pointer text-sm text-primary">
                <input id="carListingsFilter_city_{city}" type="checkbox" bind:group={selectedCities} value={city} onchange={applyFilters}
                  class="w-4 h-4 text-blue-600 rounded border-gray-300 dark:border-gray-500 dark:bg-gray-600 focus:ring-blue-500" />
                {$t(`city.${city}`)}
              </label>
            {/each}
          </div>
        {/if}
      </div>

      <!-- Color -->
      <div class="relative">
        {@render dropdownChip($t('edit.field.color'), selectedColors.length, colorOpen, () => { colorOpen = !colorOpen; brandOpen = false; cityOpen = false; })}
        {#if colorOpen}
          <button type="button" class="fixed inset-0 z-40" onclick={() => colorOpen = false} aria-label="Close"></button>
          <div class="absolute z-50 mt-1.5 w-52 surface rounded-xl shadow-lg max-h-64 overflow-y-auto py-1">
            {#each COLORS as color}
              <label for="carListingsFilter_color_{color}" class="flex items-center gap-2.5 px-3 py-2 hover:bg-surface-hover cursor-pointer text-sm text-primary">
                <input id="carListingsFilter_color_{color}" type="checkbox" bind:group={selectedColors} value={color} onchange={applyFilters}
                  class="w-4 h-4 text-blue-600 rounded border-gray-300 dark:border-gray-500 dark:bg-gray-600 focus:ring-blue-500" />
                {$t(`color.${color}`)}
              </label>
            {/each}
          </div>
        {/if}
      </div>
    </div>

    <hr class="border-gray-100 dark:border-gray-700" />

    <div class="space-y-4">
      <!-- Body type -->
      <div>
        <p class="text-xs font-medium text-muted mb-2">{$t('edit.field.bodyType')}</p>
        <div class="flex flex-wrap gap-1.5">
          {#each BODY_TYPES as bodyType}
            <button
              type="button"
              onclick={() => { selectedBodyTypes = toggleValue(selectedBodyTypes, bodyType); applyFilters(); }}
              class="px-3 py-1 text-sm rounded-full border transition-all cursor-pointer select-none focus:outline-none
                {selectedBodyTypes.includes(bodyType)
                  ? 'border-2 border-blue-500 bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400 font-medium'
                  : 'border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-700 text-body hover:border-gray-400 dark:hover:border-gray-400'}"
            >{$t(`bodyType.${bodyType}`)}</button>
          {/each}
        </div>
      </div>

      <!-- Fuel type -->
      <div>
        <p class="text-xs font-medium text-muted mb-2">{$t('edit.field.fuelType')}</p>
        <div class="flex flex-wrap gap-1.5">
          {#each FUEL_TYPES as fuel}
            <button
              type="button"
              onclick={() => { selectedFuelTypes = toggleValue(selectedFuelTypes, fuel); applyFilters(); }}
              class="px-3 py-1 text-sm rounded-full border transition-all cursor-pointer select-none focus:outline-none
                {selectedFuelTypes.includes(fuel)
                  ? 'border-2 border-blue-500 bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400 font-medium'
                  : 'border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-700 text-body hover:border-gray-400 dark:hover:border-gray-400'}"
            >{$t(`fuelType.${fuel}`)}</button>
          {/each}
        </div>
      </div>

      <!-- Transmission -->
      <div>
        <p class="text-xs font-medium text-muted mb-2">{$t('edit.field.transmission')}</p>
        <div class="flex flex-wrap gap-1.5">
          {#each TRANSMISSIONS as transmission}
            <button
              type="button"
              onclick={() => { selectedTransmissions = toggleValue(selectedTransmissions, transmission); applyFilters(); }}
              class="px-3 py-1 text-sm rounded-full border transition-all cursor-pointer select-none focus:outline-none
                {selectedTransmissions.includes(transmission)
                  ? 'border-2 border-blue-500 bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400 font-medium'
                  : 'border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-700 text-body hover:border-gray-400 dark:hover:border-gray-400'}"
            >{$t(`transmission.${transmission}`)}</button>
          {/each}
        </div>
      </div>

      <!-- Drive + Condition + Owners in a row -->
      <div class="flex flex-wrap gap-x-8 gap-y-4">
        <div>
          <p class="text-xs font-medium text-muted mb-2">{$t('edit.field.driveType')}</p>
          <div class="flex flex-wrap gap-1.5">
            {#each DRIVE_TYPES as drive}
              <button
                type="button"
                onclick={() => { selectedDriveTypes = toggleValue(selectedDriveTypes, drive); applyFilters(); }}
                class="px-3 py-1 text-sm rounded-full border transition-all cursor-pointer select-none focus:outline-none
                  {selectedDriveTypes.includes(drive)
                    ? 'border-2 border-blue-500 bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400 font-medium'
                    : 'border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-700 text-body hover:border-gray-400 dark:hover:border-gray-400'}"
              >{$t(`driveType.${drive}`)}</button>
            {/each}
          </div>
        </div>
        <div>
          <p class="text-xs font-medium text-muted mb-2">{$t('edit.field.condition')}</p>
          <div class="flex flex-wrap gap-1.5">
            {#each CONDITIONS as condition}
              <button
                type="button"
                onclick={() => { selectedConditions = toggleValue(selectedConditions, condition); applyFilters(); }}
                class="px-3 py-1 text-sm rounded-full border transition-all cursor-pointer select-none focus:outline-none
                  {selectedConditions.includes(condition)
                    ? 'border-2 border-blue-500 bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400 font-medium'
                    : 'border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-700 text-body hover:border-gray-400 dark:hover:border-gray-400'}"
              >{$t(`condition.${condition}`)}</button>
            {/each}
          </div>
        </div>
        <div>
          <p class="text-xs font-medium text-muted mb-2">{$t('edit.field.ownersCount')}</p>
          <div class="flex flex-wrap gap-1.5">
            {#each OWNERS_COUNTS as count}
              <button
                type="button"
                onclick={() => { selectedOwnersCounts = toggleValue(selectedOwnersCounts, count); applyFilters(); }}
                class="w-9 py-1 text-sm rounded-full border transition-all cursor-pointer select-none focus:outline-none text-center
                  {selectedOwnersCounts.includes(count)
                    ? 'border-2 border-blue-500 bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400 font-medium'
                    : 'border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-700 text-body hover:border-gray-400 dark:hover:border-gray-400'}"
              >{count}</button>
            {/each}
          </div>
        </div>
      </div>
    </div>

    {#if activeFilterCount > 0}
      <hr class="border-gray-100 dark:border-gray-700" />
      <Button color="light" size="sm" onclick={clearAll} class="gap-1.5">
        <CloseOutline class="w-3.5 h-3.5" />
        {$t('filters.clearAll')}
      </Button>
    {/if}
  </div>
{/if}
