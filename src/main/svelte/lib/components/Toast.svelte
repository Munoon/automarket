<script lang="ts">
	import { Toast, ToastContainer } from 'flowbite-svelte';
  import { fly } from "svelte/transition";
	import { ExclamationCircleOutline } from 'flowbite-svelte-icons';
	import { toastStore } from '$lib/stores/toastStore';
</script>

<ToastContainer position="top-right" class="fixed top-4 right-4 z-50">
	{#each $toastStore as toast (toast.id)}
		<Toast
      color="red"
      dismissable
      transition={fly}
      onclose={() => toastStore.remove(toast.id)}>
			{#snippet icon()}
				<ExclamationCircleOutline />
			{/snippet}
			<div>
				{toast.message}
			</div>
		</Toast>
	{/each}
</ToastContainer>


