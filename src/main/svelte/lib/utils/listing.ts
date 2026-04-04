import type { FuelType, TransmissionType, City } from '$lib/apiClient';
import type { TranslationKey } from '$lib/i18n';

export function fuelTypeKey(fuelType: FuelType): TranslationKey {
	return `fuelType.${fuelType}`;
}

export function transmissionKey(transmission: TransmissionType): TranslationKey {
	return `transmission.${transmission}`;
}

export function cityKey(city: City): TranslationKey {
	return `city.${city}`;
}

export function listingSlug(id: number, title: string | null): string {
	const titleSlug = title
		? '-' + title.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '')
		: '';
	return `${id}${titleSlug}`;
}

export function parseListingId(slug: string): number {
	return parseInt(slug.split('-')[0]);
}
