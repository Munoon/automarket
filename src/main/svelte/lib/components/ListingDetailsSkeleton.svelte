<div class="max-w-7xl mx-auto px-4 sm:px-6 py-8 space-y-6 animate-pulse">

  <!-- Title + Price header -->
  <div class="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3">
    <div class="flex flex-col gap-2 flex-1">
      <!-- Brand/model subtitle -->
      <div class="h-3.5 skeleton-box w-32"></div>
      <!-- Title -->
      <div class="h-8 skeleton-box w-3/4"></div>
      <!-- City + date -->
      <div class="flex gap-3 mt-1">
        <div class="h-3.5 skeleton-box w-24"></div>
        <div class="h-3.5 skeleton-box w-36"></div>
      </div>
    </div>
    <!-- Price -->
    <div class="h-9 skeleton-box w-40 shrink-0 self-start"></div>
  </div>

  <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">

    <!-- Left: Gallery + Description -->
    <div class="lg:col-span-2 space-y-6">

      <!-- Image carousel -->
      <div class="rounded-2xl overflow-hidden surface h-80 sm:h-105"></div>

      <!-- Description card -->
      <div class="info-card flex flex-col gap-3">
        <div class="h-5 skeleton-box w-28"></div>
        <div class="h-3.5 skeleton-box w-full"></div>
        <div class="h-3.5 skeleton-box w-full"></div>
        <div class="h-3.5 skeleton-box w-2/3"></div>
      </div>
    </div>

    <!-- Right: Seller card + badges + details -->
    <div class="space-y-5">

      <!-- Seller card -->
      <div class="info-card flex flex-col gap-4">
        <div class="h-5 skeleton-box w-24"></div>
        <div class="h-4 skeleton-box w-36"></div>
        <div class="h-10 skeleton-box w-full rounded-lg"></div>
      </div>

      <!-- Quick badges -->
      <div class="flex flex-wrap gap-2">
        <div class="h-5 skeleton-box w-16 rounded-full"></div>
        <div class="h-5 skeleton-box w-20 rounded-full"></div>
        <div class="h-5 skeleton-box w-12 rounded-full"></div>
        <div class="h-5 skeleton-box w-14 rounded-full"></div>
        <div class="h-5 skeleton-box w-24 rounded-full"></div>
      </div>

      <!-- Details grid card -->
      <div class="info-card flex flex-col gap-0 divide-surface">
        <div class="h-5 skeleton-box w-24 mb-4"></div>
        {#each { length: 6 } as _}
          <div class="flex justify-between py-2.5">
            <div class="h-3.5 skeleton-box w-24"></div>
            <div class="h-3.5 skeleton-box w-16"></div>
          </div>
        {/each}
      </div>

    </div>
  </div>
</div>
