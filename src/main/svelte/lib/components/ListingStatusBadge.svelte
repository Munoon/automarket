<script lang="ts">
    import { Badge } from 'flowbite-svelte';
    import { type ListingStatus } from '$lib/apiClient';
    import { listingStatusKey } from '$lib/utils/listing';
    import { t } from '$lib/i18n';

    const STATUS_COLOR: Record<ListingStatus, 'green' | 'gray' | 'yellow'> = {
        PUBLISHED: 'green',
        DRAFT:     'gray',
        ARCHIVED:  'yellow',
    };
    
    const { status }: { status: ListingStatus } = $props();
</script>

<Badge color={STATUS_COLOR[status]} rounded class="text-xs rounded-sm">
    {$t(listingStatusKey(status))}
</Badge>