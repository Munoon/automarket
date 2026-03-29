<script lang="ts">
	import { Navbar, NavBrand, Button, NavHamburger } from 'flowbite-svelte';
	import { PlusOutline, UserOutline } from 'flowbite-svelte-icons';
	import { language, t } from '$lib/i18n';

	let showLanguageDropdown = $state(false);

	function setLanguage(code: 'en' | 'uk') {
		language.set(code);
		showLanguageDropdown = false;
	}

	function handleCreateListing() {
		// TODO: Navigate to create listing page
		console.log('Create listing clicked');
	}

	function handleSignIn() {
		// TODO: Navigate to sign in page
		console.log('Sign in clicked');
	}

	function toggleLanguageDropdown() {
		showLanguageDropdown = !showLanguageDropdown;
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
		<Button 
			size="sm" 
			outline 
			onclick={handleSignIn}
			class="navbar-outline-btn border-gray-300 dark:border-gray-500 text-gray-900 dark:text-gray-100 hover:border-blue-600 dark:hover:border-blue-500 hover:text-gray-900! hover:dark:text-gray-100!"
		>
			<UserOutline class="me-1 h-4 w-4" />
			{$t('header.signIn')}
		</Button>
		
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
		
		<NavHamburger />
	</div>
</Navbar>
