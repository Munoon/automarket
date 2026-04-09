import { authStore } from '$lib/stores/authStore';

export interface RequestOptions {
	maxRetries?: number;
	retryTimeoutMS?: number;
	token?: string;
}

export interface ProblemDetails {
	type: string;
	title: string;
	status: number;
}

export class ProblemException extends Error {
	public readonly problem: ProblemDetails;

	public constructor(problem: ProblemDetails) {
		super(problem.title);
		this.name = 'ProblemException';
		this.problem = problem;
	}
}

export interface Page<T> {
	content: T[];
	totalElements: number;
}

export interface ProfileResponse {
	user: UserProfile;
	limits: Limits;
}

export interface UserProfile {
	id: number;
	phoneNumber: string;
	displayName: string | null;
	createdAt: number;
	active: boolean;
}

export interface Limits {
	listingRepublishCooldownMS: number;
	listingsCountLimitPerAuthor: number;
}

export interface AuthRequest {
	phoneNumber: string;
	code: string;
}

export interface SendVerificationCodeRequest {
	phoneNumber: string;
	captchaToken: string | null;
}

export interface SendVerificationCodeResponse {
	codeTimeToLiveSeconds: number;
}

export interface AuthResponse {
	token: string;
	tokenExpiresInSeconds: number;
	profile: UserProfile;
	limits: Limits;
}

export interface UpdateDisplayNameRequest {
	displayName: string;
}

export interface UpdateListingStatusRequest {
	status: ListingStatus;
}

export type FuelType = 'PETROL' | 'DIESEL' | 'LPG' | 'ELECTRIC' | 'HYBRID' | 'PLUG_IN_HYBRID';

export type TransmissionType = 'MANUAL' | 'AUTOMATIC' | 'CVT' | 'SEMI_AUTOMATIC';

export type BodyType = 'SEDAN' | 'HATCHBACK' | 'WAGON' | 'COUPE' | 'CONVERTIBLE' | 'SUV' |
	'CROSSOVER' | 'MINIVAN' | 'PICKUP' | 'VAN';

export type CarBrand = 'TOYOTA' | 'VOLKSWAGEN' | 'BMW' | 'MERCEDES_BENZ' | 'AUDI' |
	'SKODA' | 'HYUNDAI' | 'KIA' | 'FORD' | 'OPEL' | 'RENAULT' | 'PEUGEOT' | 'CITROEN' |
	'HONDA' | 'MAZDA' | 'NISSAN' | 'MITSUBISHI' | 'SUBARU' | 'SUZUKI' | 'LEXUS' | 'LAND_ROVER' |
	'JEEP' | 'CHEVROLET' | 'FIAT' | 'VOLVO' | 'SEAT' | 'DACIA' | 'ALFA_ROMEO' | 'PORSCHE' |
	'LADA' | 'ZAZ' | 'CUSTOM';

export type CarColor = 'WHITE' | 'BLACK' | 'SILVER' | 'GRAY' | 'RED' | 'BLUE' | 'DARK_BLUE' |
	'GREEN' | 'DARK_GREEN' | 'YELLOW' | 'ORANGE' | 'BROWN' | 'BEIGE' | 'PURPLE' | 'GOLDEN' | 'BURGUNDY';

export type CarCondition = 'NEW' | 'USED';

export type DriveType = 'FWD' | 'RWD' | 'AWD' | 'FOUR_WD';

export type ListingStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';

export type City = 'KYIV' | 'KHARKIV' | 'ODESA' | 'DNIPRO' | 'ZAPORIZHZHIA' |
    'LVIV' | 'KRYVYI_RIH' | 'MYKOLAIV' | 'MARIUPOL' | 'VINNYTSIA' | 'KHERSON' |
    'POLTAVA' | 'CHERNIHIV' | 'CHERKASY' | 'SUMY' | 'KHMELNYTSKYI' | 'IVANO_FRANKIVSK' |
    'RIVNE' | 'ZHYTOMYR' | 'TERNOPIL' | 'LUTSK' | 'UZHHOROD' | 'CHERNIVTSI' |
    'KREMENCHUK' | 'BILA_TSERKVA' | 'MELITOPOL' | 'MUKACHEVO' | 'DROHOBYCH';

export interface OwnCarListing {
	id: number;
	status: ListingStatus;
	title: string | null;
	description: string | null;
	brand: CarBrand | null;
	customBrandName: string | null;
	model: string | null;
	licensePlate: string | null;
	condition: CarCondition | null;
	mileage: number | null;
	price: number | null;
	city: City | null;
	color: CarColor | null;
	transmission: TransmissionType | null;
	fuelType: FuelType | null;
	tankVolume: number | null;
	driveType: DriveType | null;
	bodyType: BodyType | null;
	year: number | null;
	engineVolume: number | null;
	ownersCount: number | null;
	createdAt: number;
	updatedAt: number;
	publishedAt: number;
}

export interface OwnCarListingListItem {
	id: number;
	status: ListingStatus;
	title: string | null;
	price: number | null;
}

export interface UpdateOwnListingRequest {
	title: string | null;
	description: string | null;
	brand: CarBrand | null;
	customBrandName: string | null;
	model: string | null;
	licensePlate: string | null;
	condition: CarCondition | null;
	mileage: number | null;
	price: number | null;
	city: City | null;
	color: CarColor | null;
	transmission: TransmissionType | null;
	fuelType: FuelType | null;
	tankVolume: number | null;
	driveType: DriveType | null;
	bodyType: BodyType | null;
	year: number | null;
	engineVolume: number | null;
	ownersCount: number | null;
}

export interface GetOwnListingsRequest {
	offset?: number;
	size?: number;
}

export interface GetOwnListingAnalyticsRequest {
	timezone?: string;
}

export interface ListingAnalyticsDay {
	ts: number;
	impressionsCount: number;
	viewsCount: number;
	phoneRequestsCount: number;
	favouritesCount: number;
}

export interface PublicCarListing {
	id: number;
	authorDisplayName: string | null;
	title: string | null;
	description: string | null;
	brand: CarBrand | null;
	customBrandName: string | null;
	model: string | null;
	licensePlate: string | null;
	condition: CarCondition | null;
	mileage: number | null;
	price: number | null;
	city: City | null;
	color: CarColor | null;
	transmission: TransmissionType | null;
	fuelType: FuelType | null;
	tankVolume: number | null;
	driveType: DriveType | null;
	bodyType: BodyType | null;
	year: number | null;
	engineVolume: number | null;
	ownersCount: number | null;
	publishedAt: number;
}

export interface PublicCarListingItem {
	id: number;
	title: string | null;
	price: number | null;
	mileage: number | null;
	fuelType: FuelType | null;
	transmission: TransmissionType | null;
	city: City | null;
	year: number | null;
}

export interface GetPublicListingsRequest {
	publishedBefore?: number;
	offset?: number;
	size?: number;
}

export interface AuthorPhone {
	phoneNumber: string;
}

type HttpMethod = 'GET' | 'POST' | 'PATCH' | 'DELETE';
type QueryParams = object;

const DEFAULT_MAX_RETRIES = 3;
const DEFAULT_RETRY_TIMEOUT_MS = 5000;
const FALLBACK_INTERNAL_PROBLEM: ProblemDetails = {
	type: '/problems/internal-error',
	title: 'Internal Error',
	status: 500
};

class ServerProblemException extends ProblemException {}

export class ApiClient {
	public async sendVerificationCode(
		body: SendVerificationCodeRequest,
		options: RequestOptions = {}
	): Promise<SendVerificationCodeResponse> {
		return this.send<SendVerificationCodeResponse>('POST', '/api/users/send-verification-code', body, undefined, options);
	}

	public async authenticate(body: AuthRequest, options: RequestOptions = {}): Promise<AuthResponse> {
		return this.send<AuthResponse>('POST', '/api/users/auth', body, undefined, options);
	}

	public async getProfile(options: RequestOptions = {}): Promise<ProfileResponse> {
		return this.sendAuthenticated<ProfileResponse>('GET', '/api/users/profile', undefined, undefined, options);
	}

	public async updateDisplayName(
		body: UpdateDisplayNameRequest,
		options: RequestOptions = {}
	): Promise<void> {
		await this.sendAuthenticated<void>('PATCH', '/api/users/display-name', body, undefined, options);
	}

	public async createOwnListing(options: RequestOptions = {}): Promise<OwnCarListing> {
		return this.sendAuthenticated<OwnCarListing>('POST', '/api/listings/own', undefined, undefined, options);
	}

	public async getOwnListings(
		body: GetOwnListingsRequest = {},
		options: RequestOptions = {}
	): Promise<Page<OwnCarListingListItem>> {
		return this.sendAuthenticated<Page<OwnCarListingListItem>>('GET', '/api/listings/own', undefined, body, options);
	}

	public async getOwnListing(listingId: number, options: RequestOptions = {}): Promise<OwnCarListing> {
		return this.sendAuthenticated<OwnCarListing>('GET', `/api/listings/own/${listingId}`, undefined, undefined, options);
	}

	public async getOwnListingAnalytics(
		listingId: number,
		body: GetOwnListingAnalyticsRequest = {},
		options: RequestOptions = {}
	): Promise<ListingAnalyticsDay[]> {
		return this.sendAuthenticated<ListingAnalyticsDay[]>(
			'GET',
			`/api/listings/own/${listingId}/analytics`,
			undefined,
			body,
			options
		);
	}

	public async updateOwnListingStatus(
		listingId: number,
		body: UpdateListingStatusRequest,
		options: RequestOptions = {}
	): Promise<void> {
		await this.sendAuthenticated<void>('PATCH', `/api/listings/own/${listingId}/status`, body, undefined, options);
	}

	public async updateOwnListing(
		listingId: number,
		body: UpdateOwnListingRequest,
		options: RequestOptions = {}
	): Promise<OwnCarListing> {
		return this.sendAuthenticated<OwnCarListing>('PATCH', `/api/listings/own/${listingId}`, body, undefined, options);
	}

	public async deleteOwnListing(listingId: number, options: RequestOptions = {}): Promise<void> {
		await this.sendAuthenticated<void>('DELETE', `/api/listings/own/${listingId}`, undefined, undefined, options);
	}

	public async getPublicListing(listingId: number, options: RequestOptions = {}): Promise<PublicCarListing> {
		return this.send<PublicCarListing>('GET', `/api/listings/public/${listingId}`, undefined, undefined, options);
	}

	public async getPublicListingAuthorPhone(listingId: number, options: RequestOptions = {}): Promise<AuthorPhone> {
		return this.send<AuthorPhone>('GET', `/api/listings/public/${listingId}/phone`, undefined, undefined, options);
	}

	public async getPublicListings(
		body: GetPublicListingsRequest = {},
		options: RequestOptions = {}
	): Promise<Page<PublicCarListingItem>> {
		return this.send<Page<PublicCarListingItem>>('GET', '/api/listings/public', undefined, body, options);
	}

	private async send<T>(
		method: HttpMethod,
		path: string,
		body?: unknown,
		queryParams?: QueryParams,
		options: RequestOptions = {}
	): Promise<T> {
		return this.executeWithRetry<T>(method, path, body, queryParams, false, options);
	}

	private async sendAuthenticated<T>(
		method: HttpMethod,
		path: string,
		body?: unknown,
		queryParams?: QueryParams,
		options: RequestOptions = {}
	): Promise<T> {
		return this.executeWithRetry<T>(method, path, body, queryParams, true, options);
	}

	private async executeWithRetry<T>(
		method: HttpMethod,
		path: string,
		body: unknown,
		queryParams: QueryParams | undefined,
		requiresAuth: boolean,
		options: RequestOptions
	): Promise<T> {
		let lastError: unknown;
		const maxRetries = options.maxRetries ?? DEFAULT_MAX_RETRIES;
		const retryTimeoutMS = options.retryTimeoutMS ?? DEFAULT_RETRY_TIMEOUT_MS;

		for (let attempt = 0; attempt <= maxRetries; attempt++) {
			try {
				return await this.executeOnce<T>(method, path, body, queryParams, requiresAuth, options);
			} catch (error) {
				if (error instanceof ServerProblemException) {
					throw error;
				}
				lastError = error;
				if (attempt < maxRetries) {
					await sleep(retryTimeoutMS);
				}
			}
		}

		throw toProblemException(lastError);
	}

	private async executeOnce<T>(
		method: HttpMethod,
		path: string,
		body: unknown,
		queryParams: QueryParams | undefined,
		requiresAuth: boolean,
		options: RequestOptions = {}
	): Promise<T> {
		const headers = new Headers();
		headers.set('Accept', 'application/json');

		if (body !== undefined) {
			headers.set('Content-Type', 'application/json');
		}

		if (requiresAuth) {
			const token = options.token || authStore.getToken();
			if (token) {
				headers.set('Authorization', `Bearer ${token}`);
			} else {
				throw new ProblemException({
					type: '/problems/authentication-required',
					title: 'Authentication Required',
					status: 401
				});
			}
		}

		const response = await fetch(this.buildUrl(path, queryParams), {
			method,
			headers,
			body: body === undefined ? undefined : JSON.stringify(body)
		});

		if (!response.ok) {
			const serverProblem = await this.extractProblem(response);
			throw new ServerProblemException(serverProblem);
		}

		if (response.status === 204) {
			return undefined as T;
		}

		const contentType = response.headers.get('content-type') ?? '';
		if (contentType.includes('application/json')) {
			return (await response.json()) as T;
		}

		return undefined as T;
	}

	private buildUrl(path: string, queryParams?: QueryParams): string {
		const normalizedPath = path.startsWith('/') ? path : `/${path}`;
		let url = normalizedPath;

		if (queryParams) {
			const params = new URLSearchParams();
			for (const [key, rawValue] of Object.entries(queryParams)) {
				if (rawValue === null || rawValue === undefined) {
					continue;
				}
				if (Array.isArray(rawValue)) {
					for (const value of rawValue) {
						params.append(key, String(value));
					}
				} else {
					params.set(key, String(rawValue));
				}
			}
			const queryString = params.toString();
			if (queryString) {
				url += `?${queryString}`;
			}
		}

		return url;
	}

	private async extractProblem(response: Response): Promise<ProblemDetails> {
		try {
			const data = (await response.json()) as Partial<ProblemDetails>;
			if (isProblem(data)) {
				return {
					type: data.type,
					title: data.title,
					status: data.status
				};
			}
		} catch {
			// Ignore parse errors and use fallback mapping below.
		}

		return {
			type: '/problems/internal-error',
			title: response.statusText || 'Internal Server Error',
			status: response.status || 500
		};
	}
}

function isProblem(value: Partial<ProblemDetails>): value is ProblemDetails {
	return (
		typeof value.type === 'string' &&
		typeof value.title === 'string' &&
		typeof value.status === 'number'
	);
}

function toProblemException(error: unknown): ProblemException {
	if (error instanceof ProblemException) {
		return error;
	}
	return new ProblemException(FALLBACK_INTERNAL_PROBLEM);
}

function sleep(ms: number): Promise<void> {
	return new Promise((resolve) => setTimeout(resolve, ms));
}

export const apiClient = new ApiClient();
