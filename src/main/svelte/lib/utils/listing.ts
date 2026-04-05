import type { FuelType, TransmissionType, City, BodyType, CarColor, DriveType, CarCondition, CarBrand } from '$lib/apiClient';
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

export function bodyTypeKey(bodyType: BodyType): TranslationKey {
	return `bodyType.${bodyType}`;
}

export function colorKey(color: CarColor): TranslationKey {
	return `color.${color}`;
}

export function driveTypeKey(driveType: DriveType): TranslationKey {
	return `driveType.${driveType}`;
}

export function conditionKey(condition: CarCondition): TranslationKey {
	return `condition.${condition}`;
}

export function brandKey(brand: CarBrand): TranslationKey {
	return `brand.${brand}`;
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
