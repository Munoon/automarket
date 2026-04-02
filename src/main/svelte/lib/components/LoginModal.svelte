<script lang="ts">
	import { Modal, Button, Input, Label, Alert, Progressbar, Helper } from 'flowbite-svelte';
	import { ExclamationCircleOutline } from 'flowbite-svelte-icons';
	import { t } from '$lib/i18n';
	import { apiClient, type ProblemException } from '$lib/apiClient';

	interface Props {
		isOpen: boolean;
		onClose: () => void;
	}

	let { isOpen = $bindable(), onClose }: Props = $props();

	type Stage = 'phone' | 'code';

	let stage: Stage = $state('phone');
	let phoneNumber: string = $state('');
	let verificationCode: string[] = $state([]);
	let error: string | null = $state(null);
	let isLoading: boolean = $state(false);
	let timeRemaining: number = $state(0);
	let totalTimeToLive: number = $state(0);
	let timerInterval: ReturnType<typeof setInterval> | null = null;
	let verificationCodeInputsDiv: HTMLDivElement | undefined = $state();
    
	function startTimer(totalSeconds: number) {
		totalTimeToLive = totalSeconds;
		timeRemaining = totalSeconds;
		stage = 'code';

		timerInterval = setInterval(() => {
			timeRemaining--;
			if (timeRemaining <= 0) {
				clearTimer();

                stage = 'phone';
                verificationCode = [];
                error = $t('auth.codeExpired');
                isLoading = false;
			}
		}, 1000);
	}

	function clearTimer() {
		if (timerInterval) {
			clearInterval(timerInterval);
			timerInterval = null;
		}
	}

	async function handleSendCode(e: Event) {
        e.preventDefault();
        error = null;

		if (phoneNumber.length !== 9 || [...phoneNumber].some(char => char < '0' || char > '9')) {
			error = $t('auth.phoneNumberInvalid');
			return;
		}

		isLoading = true;
		try {
			const response = await apiClient.sendVerificationCode({ phoneNumber: '+380' + phoneNumber });
			startTimer(response.codeTimeToLiveSeconds);
            isLoading = false;
		} catch (err) {
			const problem = (err as ProblemException).problem;
			error = problem?.title || $t('auth.error');
			isLoading = false;
		}
	}

	async function handleVerifyCode(e?: Event) {
        e?.preventDefault();
		error = null;

        const code = verificationCode.join('');
		if (code.length < 6) {
			error = $t('auth.verificationCodeInvalid');
			return;
		}

		isLoading = true;
		try {
			await apiClient.authenticate({
				phoneNumber: '+380' + phoneNumber,
				code
			});

			// Successfully authenticated
			handleClose();
		} catch (err) {
			const problem = (err as ProblemException).problem;
            error = problem?.type === '/problems/invalid-sms-code'
                ? $t('auth.verificationCodeIncorrect')
                : (problem?.title || $t('auth.error'));
			isLoading = false;
		}
	}

    function handleVerificationCodeInput(e: Event, inputNo: number) {
        const value = (e.target as HTMLInputElement).value;
        handleVerificationCodeInput0(value, inputNo);
    }

    function handleVerificationCodeInput0(value: string, inputNo: number) {
        if (value.length === 0) {
            verificationCode[inputNo - 1] = '';
            if (inputNo > 1) {
                (verificationCodeInputsDiv?.querySelector(`input:nth-child(${inputNo - 1})`) as HTMLInputElement)?.focus();
            }
        } else {
            if (value[0] < '0' || value[0] > '9') {
                verificationCode[inputNo - 1] = '';
                return;
            }
            verificationCode[inputNo - 1] = value[0];
            (verificationCodeInputsDiv?.querySelector(`input:nth-child(${inputNo + 1})`) as HTMLInputElement)?.focus();

            if (value.length > 1 && inputNo < 6) {
                handleVerificationCodeInput0(value.substring(1), inputNo + 1);
            } else if (inputNo === 6 && verificationCode.join('').length === 6) {
                handleVerifyCode();
            }
        }
    }

    function handleVerificationCodeKeyDown(e: KeyboardEvent, inputNo: number) {
        if (e.key === 'Backspace' && !verificationCode[inputNo - 1]) {
            handleVerificationCodeInput0('', inputNo);
        }
    }

	function handleClose() {
		isOpen = false;
		onClose();

		// Reset state
		stage = 'phone';
		phoneNumber = '';
		verificationCode = [];
		error = null;
		clearTimer();
		isLoading = false;
	}

	// Cleanup timer on unmount
	$effect(() => {
		return () => clearTimer();
	});
</script>

<Modal 
	bind:open={isOpen} 
    onclose={handleClose}
	outsideclose={false} 
	size="xs"
	placement="center"
    class="m-auto"
>
	<div class="space-y-6">
		<div class="flex items-center justify-between">
			<h3 class="text-xl font-semibold text-gray-900 dark:text-white">
				{stage === 'phone' ? $t('auth.signIn') : $t('auth.verifyCode')}
			</h3>
		</div>

		{#if error}
			<Alert border class="text-red-600 border-red-600">
                {#snippet icon()}<ExclamationCircleOutline class="h-5 w-5" />{/snippet}
                {error}
			</Alert>
		{/if}

		{#if stage === 'phone'}
			<form class="space-y-4" onsubmit={handleSendCode}>
                <div class="flex">
                    <div class="z-10 inline-flex shrink-0 items-center rounded-s-lg border border-r-0 border-gray-300 bg-gray-100 px-3 py-2 text-center text-sm font-medium text-gray-500 dark:border-gray-600 dark:bg-gray-700 dark:text-white dark:focus:ring-gray-700">
                        🇺🇦 +380
                    </div>
                    <Input
                        type="tel"
                        inputmode="numeric"
                        placeholder="967584954"
                        class="rounded-l-none! placeholder-gray-500"
                        required
                        autofocus
                        disabled={isLoading}
                        bind:value={phoneNumber}
                        />
                </div>
                <Helper class="mt-2 text-sm">{$t('auth.weWillSendCode')}</Helper>
                <Button
                    class="my-2 w-full text-sm"
                    color="blue"
                    type="submit"
					disabled={isLoading}
                >
                    {isLoading ? $t('auth.sending') : $t('auth.sendCode')}
                </Button>
			</form>
		{:else if stage === 'code'}
			<form class="space-y-4" onsubmit={handleVerifyCode}>
				<div>
					<Label class="mb-2 block">{$t('auth.verificationCode')}</Label>
                    <div class='flex gap-2 items-center justify-center' bind:this={verificationCodeInputsDiv}>
                        <Input
                            type="text"
                            disabled={isLoading}
                            bind:value={verificationCode[0]}
                            class='size-12! text-center text-lg'
                            required
                            autofocus
                            oninput={(e) => handleVerificationCodeInput(e, 1)}
                            onkeydown={(e) => handleVerificationCodeKeyDown(e, 1)}
                            />
                        <Input
                            type="text"
                            disabled={isLoading}
                            bind:value={verificationCode[1]}
                            class='size-12! text-center text-lg'
                            required
                            oninput={(e) => handleVerificationCodeInput(e, 2)}
                            onkeydown={(e) => handleVerificationCodeKeyDown(e, 2)}
                            />
                        <Input
                            type="text"
                            disabled={isLoading}
                            bind:value={verificationCode[2]}
                            class='size-12! text-center text-lg'
                            required
                            oninput={(e) => handleVerificationCodeInput(e, 3)}
                            onkeydown={(e) => handleVerificationCodeKeyDown(e, 3)}
                            />
                        <Input
                            type="text"
                            disabled={isLoading}
                            bind:value={verificationCode[3]}
                            class='size-12! text-center text-lg'
                            required
                            oninput={(e) => handleVerificationCodeInput(e, 4)}
                            onkeydown={(e) => handleVerificationCodeKeyDown(e, 4)}
                            />
                        <Input
                            type="text"
                            disabled={isLoading}
                            bind:value={verificationCode[4]}
                            class='size-12! text-center text-lg'
                            required
                            oninput={(e) => handleVerificationCodeInput(e, 5)}
                            onkeydown={(e) => handleVerificationCodeKeyDown(e, 5)}
                            />
                        <Input
                            type="text"
                            disabled={isLoading}
                            bind:value={verificationCode[5]}
                            class='size-12! text-center text-lg'
                            required
                            oninput={(e) => handleVerificationCodeInput(e, 6)}
                            onkeydown={(e) => handleVerificationCodeKeyDown(e, 6)}
                            />
                    </div>
				</div>

				<div class="space-y-2">
                    <span class="text-sm font-medium text-gray-700 dark:text-gray-300">
                        {$t('auth.codeExpiresIn')}
                        <span class="font-semibold">{timeRemaining}s</span>
                    </span>
                    <Progressbar
                        progress={(timeRemaining / totalTimeToLive) * 100}
                        color='blue'
                        />
                </div>

				<Button
                    disabled={isLoading}
                    color='blue'
                    class="w-full"
                    type="submit"
                >
                    {isLoading ? $t('auth.verifying') : $t('auth.verify')}
                </Button>
			</form>
		{/if}
	</div>
</Modal>
