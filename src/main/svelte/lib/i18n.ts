import { writable, derived, type Writable } from 'svelte/store';
import { browser } from '$app/environment';

export type Language = 'en' | 'uk';

export const translations = {
	en: {
		'fuelType.PETROL': 'Petrol',
		'fuelType.DIESEL': 'Diesel',
		'fuelType.LPG': 'LPG',
		'fuelType.ELECTRIC': 'Electric',
		'fuelType.HYBRID': 'Hybrid',
		'fuelType.PLUG_IN_HYBRID': 'Plug-in Hybrid',

		'transmission.MANUAL': 'Manual',
		'transmission.AUTOMATIC': 'Automatic',
		'transmission.CVT': 'CVT',
		'transmission.SEMI_AUTOMATIC': 'Semi-automatic',

		'city.KYIV': 'Kyiv',
		'city.KHARKIV': 'Kharkiv',
		'city.ODESA': 'Odesa',
		'city.DNIPRO': 'Dnipro',
		'city.ZAPORIZHZHIA': 'Zaporizhzhia',
		'city.LVIV': 'Lviv',
		'city.KRYVYI_RIH': 'Kryvyi Rih',
		'city.MYKOLAIV': 'Mykolaiv',
		'city.MARIUPOL': 'Mariupol',
		'city.VINNYTSIA': 'Vinnytsia',
		'city.KHERSON': 'Kherson',
		'city.POLTAVA': 'Poltava',
		'city.CHERNIHIV': 'Chernihiv',
		'city.CHERKASY': 'Cherkasy',
		'city.SUMY': 'Sumy',
		'city.KHMELNYTSKYI': 'Khmelnytskyi',
		'city.IVANO_FRANKIVSK': 'Ivano-Frankivsk',
		'city.RIVNE': 'Rivne',
		'city.ZHYTOMYR': 'Zhytomyr',
		'city.TERNOPIL': 'Ternopil',
		'city.LUTSK': 'Lutsk',
		'city.UZHHOROD': 'Uzhhorod',
		'city.CHERNIVTSI': 'Chernivtsi',
		'city.KREMENCHUK': 'Kremenchuk',
		'city.BILA_TSERKVA': 'Bila Tserkva',
		'city.MELITOPOL': 'Melitopol',
		'city.MUKACHEVO': 'Mukachevo',
		'city.DROHOBYCH': 'Drohobych',

        'mileage.km': 'km',
        'currency.uah': 'UAH',

        'listings.empty': 'No listings found.',
        'error.notFound': 'Page not found',
        'error.internal': 'Internal error',
        'header.createListing': 'Create Listing',
        'header.signIn': 'Sign In',
        'header.signOut': 'Sign Out',
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
        'auth.displayName': 'Name',
        'auth.displayNameLabel': 'Your Name',
        'auth.displayNamePlaceholder': 'John Doe',
        'auth.displayNameHint': 'This name will be visible to other users.',
        'auth.displayNameRequired': 'Name is required',
        'auth.displayNameTooLong': 'Name must be 100 characters or less',
        'auth.displayNameInvalidCharacters': 'Name can only contain letters, spaces, and apostrophes',
        'auth.saving': 'Saving...',
        'auth.save': 'Save',
        'auth.error': 'An error occurred. Please try again.'
	},
	uk: {
		'fuelType.PETROL': 'Бензин',
		'fuelType.DIESEL': 'Дизель',
		'fuelType.LPG': 'Газ (ЗПГ)',
		'fuelType.ELECTRIC': 'Електро',
		'fuelType.HYBRID': 'Гібрид',
		'fuelType.PLUG_IN_HYBRID': 'Плагін-гібрид',

		'transmission.MANUAL': 'Механічна',
		'transmission.AUTOMATIC': 'Автоматична',
		'transmission.CVT': 'Варіатор',
		'transmission.SEMI_AUTOMATIC': 'Напівавтоматична',

		'city.KYIV': 'Київ',
		'city.KHARKIV': 'Харків',
		'city.ODESA': 'Одеса',
		'city.DNIPRO': 'Дніпро',
		'city.ZAPORIZHZHIA': 'Запоріжжя',
		'city.LVIV': 'Львів',
		'city.KRYVYI_RIH': 'Кривий Ріг',
		'city.MYKOLAIV': 'Миколаїв',
		'city.MARIUPOL': 'Маріуполь',
		'city.VINNYTSIA': 'Вінниця',
		'city.KHERSON': 'Херсон',
		'city.POLTAVA': 'Полтава',
		'city.CHERNIHIV': 'Чернігів',
		'city.CHERKASY': 'Черкаси',
		'city.SUMY': 'Суми',
		'city.KHMELNYTSKYI': 'Хмельницький',
		'city.IVANO_FRANKIVSK': 'Івано-Франківськ',
		'city.RIVNE': 'Рівне',
		'city.ZHYTOMYR': 'Житомир',
		'city.TERNOPIL': 'Тернопіль',
		'city.LUTSK': 'Луцьк',
		'city.UZHHOROD': 'Ужгород',
		'city.CHERNIVTSI': 'Чернівці',
		'city.KREMENCHUK': 'Кременчук',
		'city.BILA_TSERKVA': 'Біла Церква',
		'city.MELITOPOL': 'Мелітополь',
		'city.MUKACHEVO': 'Мукачево',
		'city.DROHOBYCH': 'Дрогобич',

        'mileage.km': 'км',
        'currency.uah': 'грн',

        'listings.empty': 'Оголошень не знайдено.',
        'error.notFound': 'Сторінку не знайдено',
        'error.internal': 'Внутрішня помилка',
        'header.createListing': 'Створити оголошення',
        'header.signIn': 'Увійти',
        'header.signOut': 'Вийти',
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
        'auth.displayName': 'Ім\'я',
        'auth.displayNameLabel': 'Ваше ім\'я',
        'auth.displayNamePlaceholder': 'Іван Іванов',
        'auth.displayNameHint': 'Це ім\'я буде видно іншим користувачам.',
        'auth.displayNameRequired': 'Ім\'я обов\'язкове',
        'auth.displayNameTooLong': 'Ім\'я повинно бути 100 символів або менше',
        'auth.displayNameInvalidCharacters': 'Ім\'я може містити тільки літери, пробіли та апострофи',
        'auth.saving': 'Збереження...',
        'auth.save': 'Зберегти',
        'auth.error': 'Сталася помилка. Будь ласка, спробуйте ще раз.'
	}
};

export type TranslationKey = keyof typeof translations['en'];

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