import { writable, derived, type Writable } from 'svelte/store';
import { browser } from '$app/environment';

export type Language = 'en' | 'uk';

export const translations = {
	en: {
        'header.createListing': 'Create Listing',
        'header.signIn': 'Sign In',
        'header.signOut': 'Sign Out',
        'home.welcome': 'Welcome to automarket',
        'auth.signIn': 'Sign In',
        'auth.weWillSendCode': 'We will send you a verification code to your phone number.',
        'auth.phoneNumberInvalid': 'Phone number must contain 9 digits',
        'auth.sendCode': 'Send Code',
        'auth.sending': 'Sending...',
        'auth.verifyCode': 'Verify Code',
        'auth.verificationCode': 'Verification Code',
        'auth.verificationCodeInvalid': 'Code must be 6 digits',
        'auth.verificationCodeIncorrect': 'Verification code is incorrect',
        'auth.verify': 'Verify',
        'auth.verifying': 'Verifying...',
        'auth.codeExpiresIn': 'Code expires in',
        'auth.codeExpired': 'Verification code has expired. Please request a new one.',
        'auth.error': 'An error occurred. Please try again.'
	},
	uk: {
        'header.createListing': 'Створити оголошення',
        'header.signIn': 'Увійти',
        'header.signOut': 'Вийти',
        'home.welcome': 'Ласкаво просимо до automarket',
        'auth.signIn': 'Увійти',
        'auth.weWillSendCode': 'Ми надішлемо вам код підтвердження на ваш номер телефону.',
        'auth.phoneNumberInvalid': 'Номер телефону повинен містити 9 цифр',
        'auth.sendCode': 'Надіслати код',
        'auth.sending': 'Надсилання...',
        'auth.verifyCode': 'Підтвердити код',
        'auth.verificationCode': 'Код підтвердження',
        'auth.verificationCodeInvalid': 'Код повинен складатися з 6 цифр',
        'auth.verificationCodeIncorrect': 'Неправильний код підтвердження',
        'auth.verify': 'Підтвердити',
        'auth.verifying': 'Перевірка...',
        'auth.codeExpiresIn': 'Код закінчується через',
        'auth.codeExpired': 'Код підтвердження закінчився. Будь ласка, запросіть новий.',
        'auth.error': 'Сталася помилка. Будь ласка, спробуйте ще раз.'
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