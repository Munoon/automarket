<script lang="ts">
	import './layout.css';
	import favicon from '$lib/assets/favicon.svg';
	import Header from '$lib/components/Header.svelte';
	import Toast from '$lib/components/Toast.svelte';
	import LoginModal from '$lib/components/LoginModal.svelte';
	import { onMount } from 'svelte';
	import { authStore } from '$lib/stores/authStore';
	import { apiClient } from '$lib/apiClient';

	let { children } = $props();

	onMount(() => {
		authStore.initialize(() => apiClient.getProfile());
	});
</script>

<svelte:head>
	<title>automarket</title>
	<link rel="icon" href={favicon} />
</svelte:head>

<Header />

<Toast />

<main class="min-h-screen">
	{@render children()}
</main>

<LoginModal />