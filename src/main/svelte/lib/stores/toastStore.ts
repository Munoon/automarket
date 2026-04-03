import { writable } from 'svelte/store';
import { type ProblemException } from '$lib/apiClient';

export interface ToastMessage {
	id: number;
	message: string;
}

function createToastStore() {
	const { subscribe, update } = writable<ToastMessage[]>([]);

	let toastId = 0;

  const remove = (id: number) => {
    update((toasts) => toasts.filter((t) => t.id !== id));
  };

  const addError = (message: string, timeout: number = 4000): ToastMessage => {
    const errorToast: ToastMessage = {
      id: ++toastId,
      message
    };

    update((toasts) => [...toasts, errorToast]);

    setTimeout(() => remove(errorToast.id), timeout);

    return errorToast;
  };

  const addApiError = (err: unknown) => {
    const problem = (err as ProblemException).problem;
    const errorMessage = problem?.title || 'Unknown error';
    addError(errorMessage);
  }

	return {
		subscribe,
		addError,
    addApiError,
		remove
	};
}

export const toastStore = createToastStore();

