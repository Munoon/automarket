<script lang="ts">
	import { Navbar, NavBrand, Button, Dropdown, DropdownItem } from 'flowbite-svelte';
	import { PlusOutline, UserOutline, ChevronDownOutline, ArrowRightToBracketOutline } from 'flowbite-svelte-icons';
	import { language, t, type Language } from '$lib/i18n';
	import { authStore } from '$lib/stores/authStore';
	import { apiClient } from '$lib/apiClient';
	import { toastStore } from '$lib/stores/toastStore';
	import { withAuth } from '$lib/composables/useAuthAction';
	import { goto } from '$app/navigation';

	let isCreatingListing = $state(false);
	let profileDropdownOpen = $state(false);
	let langDropdownOpen = $state(false);

	function setLanguage(code: Language) {
		language.set(code);
		langDropdownOpen = false;
	}

	async function createListing() {
		isCreatingListing = true;
		try {
			const listing = await apiClient.createOwnListing();
			isCreatingListing = false;
			await goto(`/${listing.id}/edit`);
		} catch (err) {
			isCreatingListing = false;
			toastStore.addApiError(err);
		}
	}

	function handleSignOut() {
		authStore.clearAuth();
		profileDropdownOpen = false;
	}
</script>

<Navbar class="border-b surface px-4 py-2.5">
	<NavBrand href="/">
		<span class="self-center whitespace-nowrap text-2xl font-semibold text-primary">
			automarket
		</span>
	</NavBrand>

	<div class="flex items-center gap-3 md:order-2">
		<Button size="sm" color="blue" onclick={() => withAuth(createListing)} class="!px-2 disabled:bg-blue-400 disabled:text-blue-100 disabled:cursor-not-allowed" disabled={isCreatingListing || !$authStore.initialized}>
			<PlusOutline class="me-1 h-5 w-5 stroke-2" />
			{$t('header.createListing')}
		</Button>

		{#if !$authStore.initialized}
			<div class="animate-pulse skeleton-box h-9 w-25 rounded-lg"></div>
		{:else if $authStore.profile}
			<Button
				size="sm"
				outline
				onclick={() => (profileDropdownOpen = !profileDropdownOpen)}
				class="navbar-outline-btn text-primary flex items-center gap-1"
			>
				<UserOutline class="h-4 w-4" />
				<span>{$authStore.profile.displayName || $authStore.profile.phoneNumber}</span>
				<ChevronDownOutline class="h-4 w-4" />
			</Button>
			<Dropdown bind:isOpen={profileDropdownOpen} placement="bottom-end">
				<DropdownItem onclick={handleSignOut} class="flex items-center gap-2 text-danger hover:bg-surface-hover">
					<ArrowRightToBracketOutline class="h-4 w-4" />
					{$t('header.signOut')}
				</DropdownItem>
			</Dropdown>
		{:else}
			<Button
				size="sm"
				outline
				onclick={() => withAuth(() => {})}
				class="navbar-outline-btn text-primary"
			>
				<UserOutline class="me-1 h-4 w-4" />
				{$t('header.signIn')}
			</Button>
		{/if}

		<Button
			size="sm"
			class="text-lg !p-1 hover:opacity-90"
			onclick={() => (langDropdownOpen = !langDropdownOpen)}
		>
			{$language === 'en' ? '🇬🇧' : '🇺🇦'}
		</Button>
		<Dropdown bind:isOpen={langDropdownOpen} placement="bottom-end">
			<DropdownItem onclick={() => setLanguage('en')} class="text-primary {$language === 'en' ? 'bg-surface-hover' : ''}">
				🇬🇧 English
			</DropdownItem>
			<DropdownItem onclick={() => setLanguage('uk')} class="text-primary {$language === 'uk' ? 'bg-surface-hover' : ''}">
				🇺🇦 Ukrainian
			</DropdownItem>
		</Dropdown>
	</div>
</Navbar>
