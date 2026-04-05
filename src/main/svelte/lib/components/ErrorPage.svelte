<script lang="ts">
	interface Props {
		status: number;
		message: string;
	}

	let { status, message }: Props = $props();
</script>

<div class="flex flex-col items-center justify-center py-24 text-center">
	<p class="text-8xl font-bold text-gray-200">{status}</p>
	<p class="mt-4 text-xl text-gray-600">{message}</p>
</div>
