// See https://svelte.dev/docs/kit/types#app.d.ts
// for information about these interfaces
declare global {
	namespace App {
		// interface Error {}
		// interface Locals {}
		// interface PageData {}
		// interface PageState {}
		// interface Platform {}
	}

	interface Window {
		__config: {
			hcaptchaSitekey: string;
		};
	}

	const hcaptcha: {
		render(container: string | HTMLElement, options: Record<string, unknown>): number;
		execute(widgetId: number): void;
		reset(widgetId: number): void;
		remove(widgetId: number): void;
	};
}

export {};
