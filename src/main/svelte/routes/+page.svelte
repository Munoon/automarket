<script lang="ts">
  import ListingsGrid from './ListingsGrid.svelte';
  import MyListingsStrip from './MyListingsStrip.svelte';
</script>

<svelte:head>
  <title>automarket</title>
</svelte:head>

<div class="max-w-7xl mx-auto px-6 py-8 flex flex-col gap-0">
  <MyListingsStrip />
  <ListingsGrid />
</div>
