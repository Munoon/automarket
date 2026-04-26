package edu.automarket.listing;

import edu.automarket.listing.dto.AuthorPhoneDTO;
import edu.automarket.listing.dto.GetPublishedListingsRequestDTO;
import edu.automarket.listing.dto.OwnCarListingListItemDTO;
import edu.automarket.listing.dto.PublicCarListingDTO;
import edu.automarket.listing.dto.PublicCarListingItemDTO;
import edu.automarket.listing.model.BodyType;
import edu.automarket.listing.model.CarBrand;
import edu.automarket.listing.model.CarColor;
import edu.automarket.listing.model.CarCondition;
import edu.automarket.listing.model.CarListing;
import edu.automarket.listing.model.City;
import edu.automarket.listing.model.DriveType;
import edu.automarket.listing.model.FuelType;
import edu.automarket.listing.model.ListingStatus;
import edu.automarket.listing.model.TransmissionType;
import edu.automarket.upload.UploadService;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class CarListingRepository {
    //language=postgresql
    private static final String CREATE_QUERY = """
            INSERT INTO car_listings (author_user_id, status, created_at, updated_at)
            VALUES (:authorUserId, 'DRAFT', :createdAt, :updatedAt)
            RETURNING id
            """;

    //language=postgresql
    private static final String SELECT_BY_USER_ID = """
            SELECT id, status::text, title, price
            FROM car_listings
            WHERE author_user_id = :authorUserId
            ORDER BY
                CASE status
                    WHEN 'DRAFT' THEN 1
                    WHEN 'PUBLISHED' THEN 2
                    WHEN 'ARCHIVED' THEN 3
                    ELSE 4
                END ASC,
                created_at DESC
            LIMIT :size OFFSET :offset
            """;

    //language=postgresql
    private static final String SELECT_BY_ID_QUERY = """
            SELECT id, author_user_id, status::text, title, description, image_keys, brand::text, custom_brand_name,
                   model, license_plate, condition::text, mileage, price, city::text, color::text, transmission::text,
                   fuel_type::text, tank_volume, drive_type::text, body_type::text, year, engine_volume,
                   owners_count, created_at, updated_at, published_at, promoted_until
            FROM car_listings
            WHERE id = :id
            """;

    //language=postgresql
    private static final String UPDATE_QUERY = """
            UPDATE car_listings
            SET author_user_id    = :authorUserId,
                status            = :status::listing_status,
                title             = :title,
                description       = :description,
                image_keys        = :imageKeys,
                brand             = :brand::car_brand,
                custom_brand_name = :customBrandName,
                model             = :model,
                license_plate     = :licensePlate,
                condition         = :condition::car_condition,
                mileage           = :mileage,
                price             = :price,
                city              = :city::city,
                color             = :color::car_color,
                transmission      = :transmission::transmission_type,
                fuel_type         = :fuelType::fuel_type,
                tank_volume       = :tankVolume,
                drive_type        = :driveType::drive_type,
                body_type         = :bodyType::body_type,
                year              = :year,
                engine_volume     = :engineVolume,
                owners_count      = :ownersCount,
                updated_at        = :updatedAt,
                published_at      = :publishedAt,
                promoted_until    = :promotedUntil
            WHERE id = :id
            """;

    //language=postgresql
    private static final String DELETE_BY_ID_QUERY = """
            DELETE FROM car_listings WHERE id = :id
            """;

    //language=postgresql
    private static final String COUNT_BY_USER_ID_AND_STATUSES_QUERY = """
            SELECT COUNT(*)
            FROM car_listings
            WHERE author_user_id = :authorUserId
            """;

    //language=postgresql
    private static final String SELECT_PUBLISHED_BASE = """
        SELECT
            id, title, image_keys, price, mileage, fuel_type::text, transmission::text, city::text, year,
            promoted_until > EXTRACT(EPOCH FROM NOW())::bigint * 1000
        FROM car_listings
        """;

    //language=postgresql
    private static final String COUNT_PUBLISHED_BASE =
            "SELECT COUNT(*) FROM car_listings";

    //language=postgresql
    private static final String SELECT_AUTHOR_PHONE_BY_PUBLISHED_ID_QUERY = """
            SELECT u.phone_number
            FROM car_listings cl
            JOIN users u ON cl.author_user_id = u.id
            WHERE cl.id = :id
              AND cl.status = 'PUBLISHED'
            """;

    //language=postgresql
    private static final String SELECT_PUBLISHED_BY_ID_QUERY = """
            SELECT cl.id, u.display_name, cl.title, cl.description, cl.image_keys, cl.brand::text, cl.custom_brand_name,
                   cl.model, cl.license_plate, cl.condition::text, cl.mileage, cl.price, cl.city::text,
                   cl.color::text, cl.transmission::text, cl.fuel_type::text, cl.tank_volume,
                   cl.drive_type::text, cl.body_type::text, cl.year, cl.engine_volume,
                   cl.owners_count, cl.published_at, cl.promoted_until > EXTRACT(EPOCH FROM NOW())::bigint * 1000
            FROM car_listings cl
            JOIN users u ON cl.author_user_id = u.id
            WHERE cl.id = :id
              AND cl.status = 'PUBLISHED'
            """;

    private final DatabaseClient client;
    private final UploadService uploadService;

    public CarListingRepository(DatabaseClient client, UploadService uploadService) {
        this.client = client;
        this.uploadService = uploadService;
    }

    public Mono<CarListing> create(long authorUserId, long createdAt) {
        return client.sql(CREATE_QUERY)
                .bind("authorUserId", authorUserId)
                .bind("createdAt", createdAt)
                .bind("updatedAt", createdAt)
                .map(row -> new CarListing(
                        row.get(0, Long.class),
                        authorUserId,
                        ListingStatus.DRAFT,
                        null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null,
                        null, null, null, null, null,
                        createdAt, createdAt, 0, 0
                ))
                .one();
    }

    public Flux<OwnCarListingListItemDTO> findByUserId(long authorUserId, int offset, int size) {
        return client.sql(SELECT_BY_USER_ID)
                .bind("authorUserId", authorUserId)
                .bind("offset", offset)
                .bind("size", size)
                .map(row -> new OwnCarListingListItemDTO(
                        row.get(0, Long.class),
                        ListingStatus.valueOf(row.get(1, String.class)),
                        row.get(2, String.class),
                        row.get(3, Long.class)
                ))
                .all();
    }

    public Mono<Long> countByUserIdAndStatuses(long authorUserId) {
        return client.sql(COUNT_BY_USER_ID_AND_STATUSES_QUERY)
                .bind("authorUserId", authorUserId)
                .map(row -> row.get(0, Long.class))
                .one();
    }

    public Mono<CarListing> findById(long id) {
        return client.sql(SELECT_BY_ID_QUERY)
                .bind("id", id)
                .map(row -> {
                    String brandStr = row.get(6, String.class);
                    String conditionStr = row.get(10, String.class);
                    String cityStr = row.get(13, String.class);
                    String colorStr = row.get(14, String.class);
                    String transmissionStr = row.get(15, String.class);
                    String fuelTypeStr = row.get(16, String.class);
                    BigDecimal tankVol = row.get(17, BigDecimal.class);
                    String driveTypeStr = row.get(18, String.class);
                    String bodyTypeStr = row.get(19, String.class);
                    BigDecimal engVol = row.get(21, BigDecimal.class);

                    return new CarListing(
                            row.get(0, Long.class),
                            row.get(1, Long.class),
                            ListingStatus.valueOf(row.get(2, String.class)),
                            row.get(3, String.class),
                            row.get(4, String.class),
                            row.get(5, String[].class),
                            brandStr != null ? CarBrand.valueOf(brandStr) : null,
                            row.get(7, String.class),
                            row.get(8, String.class),
                            row.get(9, String.class),
                            conditionStr != null ? CarCondition.valueOf(conditionStr) : null,
                            row.get(11, Integer.class),
                            row.get(12, Long.class),
                            cityStr != null ? City.valueOf(cityStr) : null,
                            colorStr != null ? CarColor.valueOf(colorStr) : null,
                            transmissionStr != null ? TransmissionType.valueOf(transmissionStr) : null,
                            fuelTypeStr != null ? FuelType.valueOf(fuelTypeStr) : null,
                            tankVol != null ? tankVol.doubleValue() : null,
                            driveTypeStr != null ? DriveType.valueOf(driveTypeStr) : null,
                            bodyTypeStr != null ? BodyType.valueOf(bodyTypeStr) : null,
                            row.get(20, Integer.class),
                            engVol != null ? engVol.doubleValue() : null,
                            row.get(22, Integer.class),
                            row.get(23, Long.class),
                            row.get(24, Long.class),
                            row.get(25, Long.class),
                            row.get(26, Long.class)
                    );
                })
                .one();
    }

    public Mono<Void> deleteById(long id) {
        return client.sql(DELETE_BY_ID_QUERY)
                .bind("id", id)
                .fetch().rowsUpdated().then();
    }

    public Mono<Void> update(CarListing carListing) {
        var spec = client.sql(UPDATE_QUERY)
                .bind("id", carListing.id())
                .bind("authorUserId", carListing.authorUserId())
                .bind("status", carListing.status().name())
                .bind("updatedAt", carListing.updatedAt())
                .bind("publishedAt", carListing.publishedAt())
                .bind("promotedUntil", carListing.promotedUntil());

        spec = carListing.title() != null
                ? spec.bind("title", carListing.title())
                : spec.bindNull("title", String.class);
        spec = carListing.description() != null
                ? spec.bind("description", carListing.description())
                : spec.bindNull("description", String.class);
        spec = spec.bind("imageKeys", carListing.imageKeys() != null ? carListing.imageKeys() : new String[0]);
        spec = carListing.brand() != null
                ? spec.bind("brand", carListing.brand().name())
                : spec.bindNull("brand", String.class);
        spec = carListing.customBrandName() != null
                ? spec.bind("customBrandName", carListing.customBrandName())
                : spec.bindNull("customBrandName", String.class);
        spec = carListing.model() != null
                ? spec.bind("model", carListing.model())
                : spec.bindNull("model", String.class);
        spec = carListing.licensePlate() != null
                ? spec.bind("licensePlate", carListing.licensePlate())
                : spec.bindNull("licensePlate", String.class);
        spec = carListing.condition() != null
                ? spec.bind("condition", carListing.condition().name())
                : spec.bindNull("condition", String.class);
        spec = carListing.mileage() != null
                ? spec.bind("mileage", carListing.mileage())
                : spec.bindNull("mileage", Integer.class);
        spec = carListing.price() != null
                ? spec.bind("price", carListing.price())
                : spec.bindNull("price", Long.class);
        spec = carListing.city() != null
                ? spec.bind("city", carListing.city().name())
                : spec.bindNull("city", String.class);
        spec = carListing.color() != null
                ? spec.bind("color", carListing.color().name())
                : spec.bindNull("color", String.class);
        spec = carListing.transmission() != null
                ? spec.bind("transmission", carListing.transmission().name())
                : spec.bindNull("transmission", String.class);
        spec = carListing.fuelType() != null
                ? spec.bind("fuelType", carListing.fuelType().name())
                : spec.bindNull("fuelType", String.class);
        spec = carListing.tankVolume() != null
                ? spec.bind("tankVolume", carListing.tankVolume())
                : spec.bindNull("tankVolume", Double.class);
        spec = carListing.driveType() != null
                ? spec.bind("driveType", carListing.driveType().name())
                : spec.bindNull("driveType", String.class);
        spec = carListing.bodyType() != null
                ? spec.bind("bodyType", carListing.bodyType().name())
                : spec.bindNull("bodyType", String.class);
        spec = carListing.year() != null
                ? spec.bind("year", carListing.year())
                : spec.bindNull("year", Integer.class);
        spec = carListing.engineVolume() != null
                ? spec.bind("engineVolume", carListing.engineVolume())
                : spec.bindNull("engineVolume", Double.class);
        spec = carListing.ownersCount() != null
                ? spec.bind("ownersCount", carListing.ownersCount())
                : spec.bindNull("ownersCount", Integer.class);

        return spec.fetch().rowsUpdated().then();
    }

    public Flux<PublicCarListingItemDTO> findPublished(GetPublishedListingsRequestDTO request) {
        StringBuilder sql = new StringBuilder(SELECT_PUBLISHED_BASE);
        buildFilterSql(request, sql);
        sql.append(" ORDER BY (promoted_until > EXTRACT(EPOCH FROM NOW())::bigint * 1000) DESC, ");
        if (request.hasQuery()) {
            sql.append("ts_rank(search_vector, to_tsquery('simple', (SELECT string_agg(lexeme || ':*', ' & ') FROM unnest(to_tsvector('simple', :query))))) DESC, ");
        }
        sql.append("published_at DESC, id DESC LIMIT :size OFFSET :offset");

        return bindFilters(client.sql(sql.toString()), request)
                .bind("size", request.getSize())
                .bind("offset", request.getOffset())
                .map(row -> {
                    String fuelTypeStr = row.get(5, String.class);
                    String transmissionTypeStr = row.get(6, String.class);
                    String cityStr = row.get(7, String.class);
                    return new PublicCarListingItemDTO(
                            row.get(0, Long.class),
                            row.get(1, String.class),
                            uploadService.getCdnEndpointUrl(row.get(2, String[].class)),
                            row.get(3, Long.class),
                            row.get(4, Integer.class),
                            fuelTypeStr != null ? FuelType.valueOf(fuelTypeStr) : null,
                            transmissionTypeStr != null ? TransmissionType.valueOf(transmissionTypeStr) : null,
                            cityStr != null ? City.valueOf(cityStr) : null,
                            row.get(8, Integer.class),
                            row.get(9, Boolean.class)
                    );
                })
                .all();
    }

    public Mono<Long> countPublished(GetPublishedListingsRequestDTO request) {
        StringBuilder sql = new StringBuilder(COUNT_PUBLISHED_BASE);
        buildFilterSql(request, sql);
        return bindFilters(client.sql(sql.toString()), request)
                .map(row -> row.get(0, Long.class))
                .one();
    }

    private void buildFilterSql(GetPublishedListingsRequestDTO r, StringBuilder sb) {
        sb.append(" WHERE status = 'PUBLISHED' AND published_at <= :publishedBefore");
        if (notEmpty(r.getBrand())) sb.append(" AND brand::text = ANY(:brand)");
        if (notEmpty(r.getCondition())) sb.append(" AND condition::text = ANY(:condition)");
        if (r.getMileageMin() != null) sb.append(" AND mileage >= :mileageMin");
        if (r.getMileageMax() != null) sb.append(" AND mileage <= :mileageMax");
        if (r.getPriceMin() != null) sb.append(" AND price >= :priceMin");
        if (r.getPriceMax() != null) sb.append(" AND price <= :priceMax");
        if (notEmpty(r.getCity())) sb.append(" AND city::text = ANY(:city)");
        if (notEmpty(r.getColor())) sb.append(" AND color::text = ANY(:color)");
        if (notEmpty(r.getTransmission())) sb.append(" AND transmission::text = ANY(:transmission)");
        if (notEmpty(r.getFuelType())) sb.append(" AND fuel_type::text = ANY(:fuelType)");
        if (r.getTankVolumeMin() != null) sb.append(" AND tank_volume >= :tankVolumeMin");
        if (r.getTankVolumeMax() != null) sb.append(" AND tank_volume <= :tankVolumeMax");
        if (notEmpty(r.getDriveType())) sb.append(" AND drive_type::text = ANY(:driveType)");
        if (notEmpty(r.getBodyType())) sb.append(" AND body_type::text = ANY(:bodyType)");
        if (r.getYearMin() != null) sb.append(" AND year >= :yearMin");
        if (r.getYearMax() != null) sb.append(" AND year <= :yearMax");
        if (r.getEngineVolumeMin() != null) sb.append(" AND engine_volume >= :engineVolumeMin");
        if (r.getEngineVolumeMax() != null) sb.append(" AND engine_volume <= :engineVolumeMax");
        if (r.getMinOwnersCount() != null) sb.append(" AND owners_count >= :minOwnersCount");
        if (r.getMaxOwnersCount() != null) sb.append(" AND owners_count <= :maxOwnersCount");
        if (r.hasQuery()) sb.append(" AND search_vector @@ to_tsquery('simple', (SELECT string_agg(lexeme || ':*', ' & ') FROM unnest(to_tsvector('simple', :query))))");
    }

    private DatabaseClient.GenericExecuteSpec bindFilters(DatabaseClient.GenericExecuteSpec spec, GetPublishedListingsRequestDTO r) {
        spec = spec.bind("publishedBefore", r.getPublishedBefore());
        if (notEmpty(r.getBrand())) spec = spec.bind("brand", toStringArray(r.getBrand()));
        if (notEmpty(r.getCondition())) spec = spec.bind("condition", toStringArray(r.getCondition()));
        if (r.getMileageMin() != null) spec = spec.bind("mileageMin", r.getMileageMin());
        if (r.getMileageMax() != null) spec = spec.bind("mileageMax", r.getMileageMax());
        if (r.getPriceMin() != null) spec = spec.bind("priceMin", r.getPriceMin());
        if (r.getPriceMax() != null) spec = spec.bind("priceMax", r.getPriceMax());
        if (notEmpty(r.getCity())) spec = spec.bind("city", toStringArray(r.getCity()));
        if (notEmpty(r.getColor())) spec = spec.bind("color", toStringArray(r.getColor()));
        if (notEmpty(r.getTransmission())) spec = spec.bind("transmission", toStringArray(r.getTransmission()));
        if (notEmpty(r.getFuelType())) spec = spec.bind("fuelType", toStringArray(r.getFuelType()));
        if (r.getTankVolumeMin() != null) spec = spec.bind("tankVolumeMin", r.getTankVolumeMin());
        if (r.getTankVolumeMax() != null) spec = spec.bind("tankVolumeMax", r.getTankVolumeMax());
        if (notEmpty(r.getDriveType())) spec = spec.bind("driveType", toStringArray(r.getDriveType()));
        if (notEmpty(r.getBodyType())) spec = spec.bind("bodyType", toStringArray(r.getBodyType()));
        if (r.getYearMin() != null) spec = spec.bind("yearMin", r.getYearMin());
        if (r.getYearMax() != null) spec = spec.bind("yearMax", r.getYearMax());
        if (r.getEngineVolumeMin() != null) spec = spec.bind("engineVolumeMin", r.getEngineVolumeMin());
        if (r.getEngineVolumeMax() != null) spec = spec.bind("engineVolumeMax", r.getEngineVolumeMax());
        if (r.getMinOwnersCount() != null) spec = spec.bind("minOwnersCount", r.getMinOwnersCount());
        if (r.getMaxOwnersCount() != null) spec = spec.bind("maxOwnersCount", r.getMaxOwnersCount());
        if (r.hasQuery()) spec = spec.bind("query", r.getQuery());
        return spec;
    }

    private static boolean notEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }

    private static String[] toStringArray(List<? extends Enum<?>> list) {
        String[] result = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i).name();
        }
        return result;
    }

    public Mono<AuthorPhoneDTO> findAuthorPhoneByPublishedId(long id) {
        return client.sql(SELECT_AUTHOR_PHONE_BY_PUBLISHED_ID_QUERY)
                .bind("id", id)
                .map(row -> new AuthorPhoneDTO(row.get(0, String.class)))
                .one();
    }

    public Mono<PublicCarListingDTO> findPublishedById(long id) {
        return client.sql(SELECT_PUBLISHED_BY_ID_QUERY)
                .bind("id", id)
                .map(row -> {
                    String brandStr = row.get(5, String.class);
                    String conditionStr = row.get(9, String.class);
                    String cityStr = row.get(12, String.class);
                    String colorStr = row.get(13, String.class);
                    String transmissionStr = row.get(14, String.class);
                    String fuelTypeStr = row.get(15, String.class);
                    BigDecimal tankVol = row.get(16, BigDecimal.class);
                    String driveTypeStr = row.get(17, String.class);
                    String bodyTypeStr = row.get(18, String.class);
                    BigDecimal engVol = row.get(20, BigDecimal.class);
                    return new PublicCarListingDTO(
                            row.get(0, Long.class),
                            row.get(1, String.class),
                            row.get(2, String.class),
                            row.get(3, String.class),
                            uploadService.getCdnEndpointUrl(row.get(4, String[].class)),
                            brandStr != null ? CarBrand.valueOf(brandStr) : null,
                            row.get(6, String.class),
                            row.get(7, String.class),
                            row.get(8, String.class),
                            conditionStr != null ? CarCondition.valueOf(conditionStr) : null,
                            row.get(10, Integer.class),
                            row.get(11, Long.class),
                            cityStr != null ? City.valueOf(cityStr) : null,
                            colorStr != null ? CarColor.valueOf(colorStr) : null,
                            transmissionStr != null ? TransmissionType.valueOf(transmissionStr) : null,
                            fuelTypeStr != null ? FuelType.valueOf(fuelTypeStr) : null,
                            tankVol != null ? tankVol.doubleValue() : null,
                            driveTypeStr != null ? DriveType.valueOf(driveTypeStr) : null,
                            bodyTypeStr != null ? BodyType.valueOf(bodyTypeStr) : null,
                            row.get(19, Integer.class),
                            engVol != null ? engVol.doubleValue() : null,
                            row.get(21, Integer.class),
                            row.get(22, Long.class),
                            row.get(23, Boolean.class)
                    );
                })
                .one();
    }

}
