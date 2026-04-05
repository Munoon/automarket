<script lang="ts">
  let { label, value }: { label: string; value: string | number | null | undefined } = $props();
</script>

{#if value != null}
  <div class="flex justify-between py-2.5 text-sm">
    <dt class="text-muted">{label}</dt>
    <dd class="font-medium text-primary text-right">{value}</dd>
  </div>
{/if}
