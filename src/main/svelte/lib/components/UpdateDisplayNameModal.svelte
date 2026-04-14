<script lang="ts">
    import { Modal, Button, Input, Label, Alert, Helper } from 'flowbite-svelte';
	import { ExclamationCircleOutline } from 'flowbite-svelte-icons';
	import { t } from '$lib/i18n';
	import { apiClient, type AuthResponse, type ProblemException } from '$lib/apiClient';
	import { authStore } from '$lib/stores/authStore';
	import { allowedChars } from '$lib/utils/allowedChars';

    interface Props {
        isOpen: boolean;
    }

    let { isOpen = $bindable() }: Props = $props();

	let displayName: string = $state('');
	let error: string | null = $state(null);
	let isLoading: boolean = $state(false);

    $effect(() => {
        if (isOpen) {
            displayName = $authStore.profile?.displayName || '';
            error = null;
            isLoading = false;
        }
    });
	
	async function handleSaveDisplayName(e: Event) {
		e.preventDefault();
		error = null;

		const trimmedDisplayName = displayName.trim();

		if (!trimmedDisplayName) {
			error = $t('auth.displayNameRequired');
			return;
		}

		if (trimmedDisplayName.length > 100) {
			error = $t('auth.displayNameTooLong');
			return;
		}

		if (!allowedChars(trimmedDisplayName, ['ALPHABETICAL', 'SPECIAL_SYMBOL'])) {
            error = $t('auth.displayNameInvalidCharacters');
            return;
        }

		isLoading = true;
		try {
			await apiClient.updateDisplayName({ displayName: trimmedDisplayName });
			authStore.updateProfile({ displayName: trimmedDisplayName });
			isLoading = false;
            handleClose();
		} catch (err) {
			const problem = (err as ProblemException).problem;
			error = problem?.title || $t('auth.error');
			isLoading = false;
		}
	}

    function handleClose() {
        isOpen = false;
        displayName = '';
        error = null;
        isLoading = false;
    }
</script>

<Modal
	bind:open={isOpen}
    onclose={handleClose}
	size="xs"
	placement="center"
    class="m-auto">
	<div class="space-y-6">
		<div class="flex items-center justify-between">
			<h3 class="text-xl font-semibold text-primary">
                {$t('header.updateDisplayName')}
			</h3>
		</div>

		{#if error}
			<Alert border class="text-red-600 border-red-600">
                {#snippet icon()}<ExclamationCircleOutline class="h-5 w-5" />{/snippet}
                {error}
			</Alert>
		{/if}

		<form class="space-y-4" onsubmit={handleSaveDisplayName}>
            <div>
                <Label class="mb-2 block">{$t('auth.displayNameLabel')}</Label>
                <Input
                    type="text"
                    name="display-name"
                    disabled={isLoading}
                    bind:value={displayName}
                    placeholder={$t('auth.displayNamePlaceholder')}
                    required
                    autofocus
                    />
                <Helper class="mt-2 text-sm">{$t('auth.displayNameHint')}</Helper>
            </div>

            <Button
                disabled={isLoading}
                color='blue'
                class="w-full"
                type="submit"
            >
                {isLoading ? $t('auth.saving') : $t('auth.save')}
            </Button>
        </form>
	</div>
</Modal>
