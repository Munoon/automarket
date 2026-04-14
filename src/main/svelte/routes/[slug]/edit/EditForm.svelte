<script lang="ts">
  import {
    cityKey, brandKey, conditionKey, bodyTypeKey,
    colorKey, fuelTypeKey, transmissionKey, driveTypeKey
  } from '$lib/utils/listing';
  import {
    type OwnCarListing,
    type CarBrand, type CarColor, type CarCondition, type BodyType,
    type City, type DriveType, type FuelType, type TransmissionType
  } from '$lib/apiClient';
  import { t } from '$lib/i18n';
  import { allowedChars, type CharacterType } from '$lib/utils/allowedChars';
  import { Input, Textarea, Select, Label } from 'flowbite-svelte';

  let {
    listing = $bindable(),
    hasErrors = $bindable(false),
    onchange,
  }: {
    listing: OwnCarListing;
    hasErrors: boolean;
    onchange: () => void;
  } = $props();

  function setString(field: keyof OwnCarListing, e: Event) {
    const v = (e.currentTarget as HTMLInputElement).value;
    (listing[field] as string | null) = v || null;
    onchange();
  }

  function setNumber(field: keyof OwnCarListing, e: Event) {
    const v = (e.currentTarget as HTMLInputElement).valueAsNumber;
    (listing[field] as number | null) = isNaN(v) ? null : v;
    onchange();
  }

  function checkText(
    value: string | null | undefined,
    min: number, max: number,
    types: CharacterType[]
  ): string | undefined {
    if (!value) return undefined;
    if (value.length < min) return $t('edit.validation.tooShort');
    if (value.length > max) return $t('edit.validation.tooLong');
    if (!allowedChars(value, types)) return $t('edit.validation.invalidChars');
    return undefined;
  }

  const errors = $derived({
    title: checkText(listing.title, 5, 200, ['ALPHABETICAL', 'DIGIT', 'SPECIAL_SYMBOL']),
    description: checkText(listing.description, 5, 5000, ['ALPHABETICAL', 'DIGIT', 'SPECIAL_SYMBOL']),
    customBrandName: listing.brand === 'CUSTOM' ? checkText(listing.customBrandName, 1, 100, ['ALPHABETICAL', 'DIGIT', 'SPECIAL_SYMBOL']) : undefined,
    model: checkText(listing.model, 1, 100, ['ALPHABETICAL', 'DIGIT', 'SPECIAL_SYMBOL']),
    licensePlate: checkText(listing.licensePlate, 1, 20, ['ALPHABETICAL', 'DIGIT', 'SPECIAL_SYMBOL']),
    price: listing.price != null && listing.price < 0 ? $t('edit.validation.mustBeNonNegative') : undefined,
    mileage: listing.mileage != null && listing.mileage < 0 ? $t('edit.validation.mustBeNonNegative') : undefined,
    ownersCount: listing.ownersCount != null && listing.ownersCount < 0 ? $t('edit.validation.mustBeNonNegative') : undefined,
    tankVolume: listing.tankVolume != null && listing.tankVolume <= 0 ? $t('edit.validation.mustBePositive') : undefined,
    engineVolume: listing.engineVolume != null && listing.engineVolume <= 0 ? $t('edit.validation.mustBePositive') : undefined,
    year: listing.year != null && (listing.year < 1900 || listing.year > 2030) ? $t('edit.validation.yearRange') : undefined,
  });

  $effect(() => {
    hasErrors = Object.values(errors).some(Boolean);
  });

  const CITIES: City[] = [
    'KYIV','KHARKIV','ODESA','DNIPRO','ZAPORIZHZHIA','LVIV','KRYVYI_RIH',
    'MYKOLAIV','MARIUPOL','VINNYTSIA','KHERSON','POLTAVA','CHERNIHIV',
    'CHERKASY','SUMY','KHMELNYTSKYI','IVANO_FRANKIVSK','RIVNE','ZHYTOMYR',
    'TERNOPIL','LUTSK','UZHHOROD','CHERNIVTSI','KREMENCHUK','BILA_TSERKVA',
    'MELITOPOL','MUKACHEVO','DROHOBYCH'
  ];
  const BRANDS: CarBrand[] = [
    'TOYOTA','VOLKSWAGEN','BMW','MERCEDES_BENZ','AUDI','SKODA','HYUNDAI',
    'KIA','FORD','OPEL','RENAULT','PEUGEOT','CITROEN','HONDA','MAZDA',
    'NISSAN','MITSUBISHI','SUBARU','SUZUKI','LEXUS','LAND_ROVER','JEEP',
    'CHEVROLET','FIAT','VOLVO','SEAT','DACIA','ALFA_ROMEO','PORSCHE',
    'LADA','ZAZ','CUSTOM'
  ];
  const BODY_TYPES: BodyType[] = ['SEDAN','HATCHBACK','WAGON','COUPE','CONVERTIBLE','SUV','CROSSOVER','MINIVAN','PICKUP','VAN'];
  const COLORS: CarColor[] = ['WHITE','BLACK','SILVER','GRAY','RED','BLUE','DARK_BLUE','GREEN','DARK_GREEN','YELLOW','ORANGE','BROWN','BEIGE','PURPLE','GOLDEN','BURGUNDY'];
  const FUEL_TYPES: FuelType[] = ['PETROL','DIESEL','LPG','ELECTRIC','HYBRID','PLUG_IN_HYBRID'];
  const TRANSMISSIONS: TransmissionType[] = ['MANUAL','AUTOMATIC','CVT','SEMI_AUTOMATIC'];
  const DRIVE_TYPES: DriveType[] = ['FWD','RWD','AWD','FOUR_WD'];
  const CONDITIONS: CarCondition[] = ['NEW','USED'];
</script>

<div class="space-y-5">

  <!-- Basic Info -->
  <section class="info-card space-y-4">
    <h2 class="text-sm font-semibold text-muted uppercase tracking-wide">
      {$t('edit.section.basic')}
    </h2>

    <div>
      <Label class="mb-1.5 block" for='listing_title'>{$t('edit.field.title')}</Label>
      <Input value={listing.title ?? undefined} oninput={(e) => setString('title', e)} id='listing_title'
        color={errors.title ? 'red' : undefined}
        placeholder={$t('edit.field.titlePlaceholder')} />
      {#if errors.title}<p class="mt-1 text-xs text-red-500">{errors.title}</p>{/if}
    </div>

    <div>
      <Label class="mb-1.5 block" for='listing_price'>{$t('edit.field.price')}</Label>
      <Input type="number" value={listing.price ?? ''} oninput={(e) => setNumber('price', e)} id='listing_price'
        color={errors.price ? 'red' : undefined}
        min="0" placeholder="0" />
      {#if errors.price}<p class="mt-1 text-xs text-red-500">{errors.price}</p>{/if}
    </div>

    <div>
      <Label class="mb-1.5 block" for='listing_description'>{$t('edit.field.description')}</Label>
      <Textarea rows={5} value={listing.description ?? undefined} oninput={(e) => setString('description', e)} class="w-full" id='listing_description'
        color={errors.description ? 'red' : undefined}
        placeholder={$t('edit.field.descriptionPlaceholder')} />
      {#if errors.description}<p class="mt-1 text-xs text-red-500">{errors.description}</p>{/if}
    </div>
  </section>

  <!-- Location -->
  <section class="info-card space-y-4">
    <h2 class="text-sm font-semibold text-muted uppercase tracking-wide">
      {$t('edit.section.location')}
    </h2>

    <div>
      <Label class="mb-1.5 block" for='listing_city'>{$t('edit.field.city')}</Label>
      <Select bind:value={listing.city} onchange={onchange} id='listing_city'>
        <option value={null} disabled>—</option>
        {#each CITIES as c}<option value={c}>{$t(cityKey(c))}</option>{/each}
      </Select>
    </div>
  </section>

  <!-- Car Details -->
  <section class="info-card space-y-4">
    <h2 class="text-sm font-semibold text-muted uppercase tracking-wide">
      {$t('edit.section.carDetails')}
    </h2>

    <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
      <div>
        <Label class="mb-1.5 block" for='listing_brand'>{$t('edit.field.brand')}</Label>
        <Select bind:value={listing.brand} onchange={onchange} id='listing_brand'>
          <option value={null} disabled>—</option>
          {#each BRANDS as b}<option value={b}>{$t(brandKey(b))}</option>{/each}
        </Select>
      </div>

      {#if listing.brand === 'CUSTOM'}
        <div>
          <Label class="mb-1.5 block" for='listing_customBrand'>{$t('edit.field.customBrand')}</Label>
          <Input value={listing.customBrandName ?? undefined} oninput={(e) => setString('customBrandName', e)} id='listing_customBrand'
            placeholder='Tesla'
            color={errors.customBrandName ? 'red' : undefined} />
          {#if errors.customBrandName}<p class="mt-1 text-xs text-red-500">{errors.customBrandName}</p>{/if}
        </div>
      {/if}

      <div>
        <Label class="mb-1.5 block" for='listing_model'>{$t('edit.field.model')}</Label>
        <Input value={listing.model ?? undefined} oninput={(e) => setString('model', e)} placeholder="e.g. Camry, Golf, A4" id='listing_model'
          color={errors.model ? 'red' : undefined} />
        {#if errors.model}<p class="mt-1 text-xs text-red-500">{errors.model}</p>{/if}
      </div>

      <div>
        <Label class="mb-1.5 block" for='listing_year'>{$t('edit.field.year')}</Label>
        <Input type="number" value={listing.year ?? ''} oninput={(e) => setNumber('year', e)} id='listing_year'
          color={errors.year ? 'red' : undefined}
          min="1900" max={new Date().getFullYear() + 1} placeholder="2020" />
        {#if errors.year}<p class="mt-1 text-xs text-red-500">{errors.year}</p>{/if}
      </div>

      <div>
        <Label class="mb-1.5 block" for='listing_condition'>{$t('edit.field.condition')}</Label>
        <Select bind:value={listing.condition} onchange={onchange} id='listing_condition'>
          <option value={null} disabled>—</option>
          {#each CONDITIONS as c}<option value={c}>{$t(conditionKey(c))}</option>{/each}
        </Select>
      </div>

      <div>
        <Label class="mb-1.5 block" for='listing_bodyType'>{$t('edit.field.bodyType')}</Label>
        <Select bind:value={listing.bodyType} onchange={onchange} id='listing_bodyType'>
          <option value={null} disabled>—</option>
          {#each BODY_TYPES as bt}<option value={bt}>{$t(bodyTypeKey(bt))}</option>{/each}
        </Select>
      </div>

      <div>
        <Label class="mb-1.5 block" for='listing_color'>{$t('edit.field.color')}</Label>
        <Select bind:value={listing.color} onchange={onchange} id='listing_color'>
          <option value={null} disabled>—</option>
          {#each COLORS as c}<option value={c}>{$t(colorKey(c))}</option>{/each}
        </Select>
      </div>
    </div>
  </section>

  <!-- Technical -->
  <section class="info-card space-y-4">
    <h2 class="text-sm font-semibold text-muted uppercase tracking-wide">
      {$t('edit.section.technical')}
    </h2>

    <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
      <div>
        <Label class="mb-1.5 block" for='listing_fuelType'>{$t('edit.field.fuelType')}</Label>
        <Select bind:value={listing.fuelType} onchange={onchange} id='listing_fuelType'>
          <option value={null} disabled>—</option>
          {#each FUEL_TYPES as f}<option value={f}>{$t(fuelTypeKey(f))}</option>{/each}
        </Select>
      </div>

      <div>
        <Label class="mb-1.5 block" for='listing_transmission'>{$t('edit.field.transmission')}</Label>
        <Select bind:value={listing.transmission} onchange={onchange} id='listing_transmission'>
          <option value={null} disabled>—</option>
          {#each TRANSMISSIONS as tr}<option value={tr}>{$t(transmissionKey(tr))}</option>{/each}
        </Select>
      </div>

      <div>
        <Label class="mb-1.5 block" for='listing_driveType'>{$t('edit.field.driveType')}</Label>
        <Select bind:value={listing.driveType} onchange={onchange} id='listing_driveType'>
          <option value={null} disabled>—</option>
          {#each DRIVE_TYPES as d}<option value={d}>{$t(driveTypeKey(d))}</option>{/each}
        </Select>
      </div>

      <div>
        <Label class="mb-1.5 block" for='listing_mileage'>{$t('edit.field.mileage')}</Label>
        <Input type="number" value={listing.mileage ?? ''} oninput={(e) => setNumber('mileage', e)}
          color={errors.mileage ? 'red' : undefined}
          min="0" placeholder="0"
          id='listing_mileage' />
        {#if errors.mileage}<p class="mt-1 text-xs text-red-500">{errors.mileage}</p>{/if}
      </div>

      <div>
        <Label class="mb-1.5 block" for='listing_engineVolume'>{$t('edit.field.engineVolume')}</Label>
        <Input type="number" value={listing.engineVolume ?? ''} oninput={(e) => setNumber('engineVolume', e)}
          color={errors.engineVolume ? 'red' : undefined}
          min="0" step="0.1" placeholder="2.0"
          id='listing_engineVolume' />
        {#if errors.engineVolume}<p class="mt-1 text-xs text-red-500">{errors.engineVolume}</p>{/if}
      </div>

      <div>
        <Label class="mb-1.5 block" for='listing_tankVolume'>{$t('edit.field.tankVolume')}</Label>
        <Input type="number" value={listing.tankVolume ?? ''} oninput={(e) => setNumber('tankVolume', e)}
          color={errors.tankVolume ? 'red' : undefined}
          min="0" placeholder="60"
          id='listing_tankVolume' />
        {#if errors.tankVolume}<p class="mt-1 text-xs text-red-500">{errors.tankVolume}</p>{/if}
      </div>
    </div>
  </section>

  <!-- Additional -->
  <section class="info-card space-y-4">
    <h2 class="text-sm font-semibold text-muted uppercase tracking-wide">
      {$t('edit.section.additional')}
    </h2>

    <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
      <div>
        <Label class="mb-1.5 block" for='listing_ownersCount'>{$t('edit.field.ownersCount')}</Label>
        <Input type="number" value={listing.ownersCount ?? ''} oninput={(e) => setNumber('ownersCount', e)}
          color={errors.ownersCount ? 'red' : undefined}
          min="1" placeholder="1"
          id='listing_ownersCount' />
        {#if errors.ownersCount}<p class="mt-1 text-xs text-red-500">{errors.ownersCount}</p>{/if}
      </div>

      <div>
        <Label class="mb-1.5 block" for='listing_licensePlate'>{$t('edit.field.licensePlate')}</Label>
        <Input value={listing.licensePlate ?? undefined} oninput={(e) => setString('licensePlate', e)} placeholder="AA 1234 BB"
          color={errors.licensePlate ? 'red' : undefined}
          id='listing_licensePlate' />
        {#if errors.licensePlate}<p class="mt-1 text-xs text-red-500">{errors.licensePlate}</p>{/if}
      </div>
    </div>
  </section>
</div>
