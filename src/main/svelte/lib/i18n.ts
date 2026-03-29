import { writable, derived, type Writable } from 'svelte/store';
import { browser } from '$app/environment';

type Language = 'en' | 'uk';

export const translations = {
	en: {
        'header.createListing': 'Create Listing',
        'header.signIn': 'Sign In',
        'home.welcome': 'Welcome to automarket'
	},
	uk: {
        'header.createListing': 'Створити оголошення',
        'header.signIn': 'Увійти',
        'home.welcome': 'Ласкаво просимо до automarket'
	}
};

type TranslationKey = keyof typeof translations['en'];

function createLanguageStore(): Writable<Language> {
	// Get language from localStorage if available, otherwise default to 'en'
	const stored = browser ? localStorage.getItem('language') : null;
	const initialLanguage = (stored as Language) || 'en';

	return writable<Language>(initialLanguage);
}

export const language = createLanguageStore();

// Create a derived store that provides a translation function
export const t = derived(language, (lang) => {
	return (path: TranslationKey): string => {
		return translations[lang][path] || path;
	};
});

// Update the document lang attribute and localStorage whenever language changes
if (browser) {
	language.subscribe((lang) => {
		localStorage.setItem('language', lang);
		document.documentElement.lang = lang;
	});
}