<script lang="ts">
	import { Navbar, NavBrand, Button } from 'flowbite-svelte';
	import { PlusOutline, UserOutline, ChevronDownOutline, ArrowRightToBracketOutline } from 'flowbite-svelte-icons';
	import { language, t, type Language } from '$lib/i18n';
	import { authStore } from '$lib/stores/authStore';
	import LoginModal from './LoginModal.svelte';

	let showLanguageDropdown = $state(false);
	let showLoginModal = $state(false);
	let showProfileDropdown = $state(false);

	$effect(() => {
		// Close profile dropdown when clicking outside would happen, but we rely on the button state
		if (showLoginModal) {
			showProfileDropdown = false;
		}
	});

	function setLanguage(code: Language) {
		language.set(code);
		showLanguageDropdown = false;
	}

	function handleCreateListing() {
		// TODO: Navigate to create listing page
		console.log('Create listing clicked');
	}

	function handleSignIn() {
		showLoginModal = true;
	}

	function handleCloseLoginModal() {
		showLoginModal = false;
	}

	function handleSignOut() {
		authStore.clearAuth();
		showProfileDropdown = false;
	}

	function toggleLanguageDropdown() {
		showLanguageDropdown = !showLanguageDropdown;
	}

	function toggleProfileDropdown() {
		showProfileDropdown = !showProfileDropdown;
	}
</script>

<Navbar class="border-b border-gray-200 bg-white px-4 py-2.5 dark:border-gray-700 dark:bg-gray-800">
	<NavBrand href="/">
		<span class="self-center whitespace-nowrap text-2xl font-semibold text-gray-900 dark:text-white">
			automarket
		</span>
	</NavBrand>

	<div class="flex items-center gap-3 md:order-2">
		<Button size="sm" color="blue" onclick={handleCreateListing} class="!px-2">
			<PlusOutline class="me-1 h-5 w-5 stroke-2" />
			{$t('header.createListing')}
		</Button>

		{#if !$authStore.initialized}
			<!-- Skeleton -->
			<div class="animate-pulse rounded-lg bg-gray-200 dark:bg-gray-700 h-9 w-25"></div>
		{:else if $authStore.profile}
			<div class="relative">
				<Button
					size="sm"
					outline
					onclick={toggleProfileDropdown}
					class="navbar-outline-btn border-gray-300 dark:border-gray-500 text-gray-900 dark:text-gray-100 hover:border-blue-600 dark:hover:border-blue-500 hover:text-gray-900! hover:dark:text-gray-100! flex items-center gap-1"
				>
					<UserOutline class="h-4 w-4" />
					<span>{$authStore.profile.displayName || $authStore.profile.phoneNumber}</span>
					<ChevronDownOutline class="h-4 w-4" />
				</Button>

				{#if showProfileDropdown}
					<div class="absolute right-0 mt-2 w-48 rounded-lg border border-gray-200 bg-white shadow-lg overflow-hidden dark:border-gray-600 dark:bg-gray-700 z-50">
						<button
							onclick={handleSignOut}
							class="flex items-center gap-2 w-full px-4 py-2 text-left text-sm text-red-600 hover:bg-gray-100 dark:text-red-500 dark:hover:bg-gray-600"
						>
							<ArrowRightToBracketOutline class="h-4 w-4" />
							{$t('header.signOut')}
						</button>
					</div>
				{/if}
			</div>
		{:else}
			<Button 
				size="sm" 
				outline 
				onclick={handleSignIn}
				class="navbar-outline-btn border-gray-300 dark:border-gray-500 text-gray-900 dark:text-gray-100 hover:border-blue-600 dark:hover:border-blue-500 hover:text-gray-900! hover:dark:text-gray-100!"
			>
				<UserOutline class="me-1 h-4 w-4" />
				{$t('header.signIn')}
			</Button>
		{/if}
		
		<div class="relative">
			<Button
				size="sm"
				class="text-lg !p-1 hover:opacity-90"
				onclick={toggleLanguageDropdown}
			>
				{$language === 'en' ? '🇬🇧' : '🇺🇦'}
			</Button>
			{#if showLanguageDropdown}
				<div class="absolute right-0 mt-1 w-auto min-w-max rounded-lg border border-gray-200 bg-white shadow-lg overflow-hidden dark:border-gray-600 dark:bg-gray-700">
					<button
							onclick={() => setLanguage('en')}
							class="block w-full px-4 py-2 text-left text-sm text-gray-900 hover:bg-gray-100 dark:text-gray-100 dark:hover:bg-gray-600 {$language === 'en' ? 'bg-gray-100 dark:bg-gray-600' : ''}"
					>
							🇬🇧 English
					</button>
					<button
							onclick={() => setLanguage('uk')}
							class="block w-full px-4 py-2 text-left text-sm text-gray-900 hover:bg-gray-100 dark:text-gray-100 dark:hover:bg-gray-600 {$language === 'uk' ? 'bg-gray-100 dark:bg-gray-600' : ''}"
					>
							🇺🇦 Ukrainian
					</button>
				</div>
			{/if}
		</div>
	</div>
</Navbar>

<LoginModal bind:isOpen={showLoginModal} onClose={handleCloseLoginModal} />