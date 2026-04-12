<script lang="ts">
  import { page } from '$app/state';
  import { goto } from '$app/navigation';
  import { parseListingId } from '$lib/utils/listing';
  import { authStore } from '$lib/stores/authStore';
  import { apiClient, ProblemException, type ListingAnalyticsDay, type OwnCarListing } from '$lib/apiClient';
  import { t, language } from '$lib/i18n';
  import { Button, Spinner } from 'flowbite-svelte';
  import { ArrowLeftOutline } from 'flowbite-svelte-icons';
  import { Chart } from '@flowbite-svelte-plugins/chart';
  import ErrorPage from '$lib/components/ErrorPage.svelte';
  import type { ApexOptions } from 'apexcharts';

  let analytics = $state<ListingAnalyticsDay[] | null>(null);
  let listing = $state<OwnCarListing | null>(null);
  let loadError = $state<{ status: number; message: string } | null>(null);
  let loading = $state(true);

  const listingId = $derived(parseListingId(page.params.slug ?? ''));
  const authInitialized = $derived($authStore.initialized);
  const authToken = $derived($authStore.token);

  $effect(() => {
    if (!authInitialized) return;
    if (!authToken) { goto('/'); return; }

    analytics = null;
    listing = null;
    loadError = null;
    loading = true;

    if (Number.isNaN(listingId)) {
      loadError = { status: 404, message: $t('error.notFound') };
      loading = false;
      return;
    }

    (async () => {
      try {
        [analytics, listing] = await Promise.all([
          apiClient.getOwnListingAnalytics(listingId, {
            timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
          }),
          apiClient.getOwnListing(listingId),
        ]);
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

  // Fill gaps so every day in the range has a data point (zeros for missing days).
  let filledAnalytics = $derived.by(() => {
    if (!analytics || analytics.length === 0) return analytics ?? [];
    const secondsInDay = 86400;
    const byTs = new Map(analytics.map(d => [d.ts, d]));
    const min = analytics[0].ts;
    const max = analytics[analytics.length - 1].ts;
    const result: ListingAnalyticsDay[] = [];
    for (let ts = min; ts <= max; ts += secondsInDay) {
      result.push(byTs.get(ts) ?? { ts, impressionsCount: 0, viewsCount: 0, phoneRequestsCount: 0, favouritesCount: 0 });
    }
    return result;
  });

  let totalImpressions = $derived(filledAnalytics.reduce((s, d) => s + d.impressionsCount, 0));
  let totalViews = $derived(filledAnalytics.reduce((s, d) => s + d.viewsCount, 0));
  let totalPhoneRequests = $derived(filledAnalytics.reduce((s, d) => s + d.phoneRequestsCount, 0));
  let totalFavourites = $derived(filledAnalytics.reduce((s, d) => s + d.favouritesCount, 0));
  let openRate = $derived(
    totalImpressions > 0 ? (totalViews / totalImpressions * 100).toFixed(1) : '0.0'
  );

  let dates = $derived(
    filledAnalytics.map(d =>
      new Date(d.ts * 1000).toLocaleDateString($language, { month: 'short', day: 'numeric' })
    )
  );

  function tooltipRow(color: string, label: string, value: string): string {
    return `
      <div class="apexcharts-tooltip-series-group" style="display:flex;align-items:center;padding:4px 12px;">
        <span class="apexcharts-tooltip-marker" style="background:${color};width:10px;height:10px;border-radius:50%;display:inline-block;margin-right:6px;flex-shrink:0;"></span>
        <div class="apexcharts-tooltip-text" style="display:flex;gap:4px;">
          <span class="apexcharts-tooltip-text-y-label" style="color:#6b7280;">${label}:</span>
          <span class="apexcharts-tooltip-text-y-value" style="font-weight:600;">${value}</span>
        </div>
      </div>
    `;
  }

  // TODO: fix legend text being black in dark mode
  function makeChartOptions(
    series: { name: string; data: number[] }[],
    colors: string[],
    extraTooltipRows?: (dataPointIndex: number, series: number[][]) => string
  ): ApexOptions {
    return {
      chart: {
        type: 'area',
        height: 180,
        toolbar: { show: false },
        zoom: { enabled: false },
        fontFamily: 'inherit',
        background: 'transparent',
        sparkline: { enabled: false },
      },
      series,
      xaxis: {
        categories: dates,
        labels: {
          style: { fontSize: '11px', colors: '#9ca3af' },
          hideOverlappingLabels: true,
          rotate: 0,
        },
        axisBorder: { show: false },
        axisTicks: { show: false },
        tooltip: { enabled: false },
      },
      yaxis: {
        labels: {
          style: { fontSize: '11px', colors: '#9ca3af' },
          formatter: v => Math.round(v).toString(),
        },
        min: 0,
      },
      colors,
      stroke: { curve: 'smooth', width: 2 },
      fill: {
        type: series.map((_, i) => i === 0 ? 'solid' : 'gradient') as any,
        opacity: series.map((_, i) => i === 0 ? 0.15 : 1) as any,
        gradient: { shadeIntensity: 1, opacityFrom: 0.25, opacityTo: 0.02, stops: [0, 100] },
      },
      dataLabels: { enabled: false },
      grid: { strokeDashArray: 4, borderColor: '#e5e7eb' },
      legend: {
        show: series.length > 1,
        position: 'top',
        horizontalAlign: 'right',
        fontSize: '12px',
      },
      tooltip: {
        x: { show: true },
        ...(extraTooltipRows ? {
          custom: ({ series: s, dataPointIndex }: { series: number[][], dataPointIndex: number }) => {
            const title = dates[dataPointIndex] ?? '';
            const rows = s.map((vals: number[], i: number) =>
              tooltipRow(colors[i], series[i].name, String(vals[dataPointIndex]))
            ).join('');
            const extra = extraTooltipRows(dataPointIndex, s);
            return `<div style="padding-bottom:4px;">
              <div style="padding:6px 12px;font-weight:600;border-bottom:1px solid #e5e7eb;font-size:12px;">${title}</div>
              ${rows}${extra}
            </div>`;
          }
        } : {}),
      },
      theme: { mode: 'light' },
    };
  }

  let viewsOptions = $derived(
    analytics
      ? makeChartOptions(
          [
            { name: $t('analytics.impressions'), data: filledAnalytics.map(d => d.impressionsCount) },
            { name: $t('analytics.detailViews'), data: filledAnalytics.map(d => d.viewsCount) },
          ],
          ['#6366f1', '#10b981'],
          (i, s) => {
            const imp = s[0][i];
            const views = s[1][i];
            const rate = imp > 0 ? (views / imp * 100).toFixed(1) : '0.0';
            return tooltipRow('#a855f7', $t('analytics.openRate'), `${rate}%`);
          }
        )
      : null
  );

  let phoneOptions = $derived(
    analytics
      ? makeChartOptions(
          [{ name: $t('analytics.phoneRequests'), data: filledAnalytics.map(d => d.phoneRequestsCount) }],
          ['#f59e0b']
        )
      : null
  );

  let favouritesOptions = $derived(
    analytics
      ? makeChartOptions(
          [{ name: $t('analytics.favourites'), data: filledAnalytics.map(d => d.favouritesCount) }],
          ['#ec4899']
        )
      : null
  );
</script>

<svelte:head>
  {#if listing}
    <title>{listing.title ?? $t('edit.untitled')} — {$t('analytics.title')}</title>
  {/if}
</svelte:head>

{#if loading}
  <div class="flex justify-center items-center py-32">
    <Spinner size="12" />
  </div>
{:else if loadError}
  <ErrorPage status={loadError.status} message={loadError.message} />
{:else if analytics && listing}
  <!-- Header -->
  <div class="sticky top-0 z-20 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 px-4 py-2.5 shadow-sm">
    <div class="max-w-7xl mx-auto flex items-center gap-2.5">
      <Button color="light" size="sm" class="p-1.5! shrink-0" onclick={() => goto(`/${listingId}/edit`)}>
        <ArrowLeftOutline class="w-4 h-4" />
      </Button>
      <span class="text-sm font-medium text-primary truncate">
        {listing.title ?? $t('edit.untitled')}
      </span>
      <span class="text-sm text-muted shrink-0">— {$t('analytics.title')}</span>
    </div>
  </div>

  <!-- Cards -->
  <div class="max-w-7xl mx-auto px-4 sm:px-6 py-6 grid grid-cols-1 sm:grid-cols-2 gap-6">

    <!-- Views -->
    <div class="info-card sm:col-span-2">
      <p class="text-xs font-semibold text-muted uppercase tracking-wide mb-4">
        {$t('analytics.views')}
      </p>
      <div class="flex gap-6 mb-2">
        <div>
          <p class="text-2xl font-bold text-primary">{totalImpressions.toLocaleString()}</p>
          <p class="text-xs text-muted mt-0.5">{$t('analytics.impressions')}</p>
        </div>
        <div>
          <p class="text-2xl font-bold text-primary">{totalViews.toLocaleString()}</p>
          <p class="text-xs text-muted mt-0.5">{$t('analytics.detailViews')}</p>
        </div>
        <div>
          <p class="text-2xl font-bold text-primary">{openRate}%</p>
          <p class="text-xs text-muted mt-0.5">{$t('analytics.openRate')}</p>
        </div>
      </div>
      <Chart options={viewsOptions!} />
    </div>

    <!-- Phone Requests -->
    <div class="info-card">
      <p class="text-xs font-semibold text-muted uppercase tracking-wide mb-4">
        {$t('analytics.phoneRequests')}
      </p>
      <div class="mb-2">
        <p class="text-2xl font-bold text-primary">{totalPhoneRequests.toLocaleString()}</p>
        <p class="text-xs text-muted mt-0.5">{$t('analytics.total')}</p>
      </div>
      <Chart options={phoneOptions!} />
    </div>

    <!-- Favourites -->
    <div class="info-card">
      <p class="text-xs font-semibold text-muted uppercase tracking-wide mb-4">
        {$t('analytics.favourites')}
      </p>
      <div class="mb-2">
        <p class="text-2xl font-bold text-primary">{totalFavourites.toLocaleString()}</p>
        <p class="text-xs text-muted mt-0.5">{$t('analytics.total')}</p>
      </div>
      <Chart options={favouritesOptions!} />
    </div>
  </div>
{/if}
