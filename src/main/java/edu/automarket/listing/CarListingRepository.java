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
import io.r2dbc.spi.Readable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Repository
public class CarListingRepository {
    //language=postgresql
    private static final String CREATE_QUERY = """
            INSERT INTO car_listings (author_user_id, status, created_at, updated_at)
            VALUES (:authorUserId, 'DRAFT', :createdAt, :updatedAt)
            RETURNING id
            """;

    //language=postgresql
    private static final String SELECT_BY_USER_ID_AND_STATUSES_QUERY = """
            SELECT id, status::text, title, published_at,
                   COALESCE(impressions_count, 0), COALESCE(views_count, 0),
                   COALESCE(phone_requests_count, 0), COALESCE(favourites_count, 0)
            FROM car_listings
            LEFT JOIN (
                SELECT listing_id,
                       sum(impressions_count) AS impressions_count,
                       sum(views_count) AS views_count,
                       sum(phone_requests_count) AS phone_requests_count,
                       sum(favourites_count) AS favourites_count
                FROM car_listing_analytics
                WHERE listing_id IN (SELECT id FROM car_listings WHERE author_user_id = :authorUserId)
                GROUP BY listing_id
            ) an ON an.listing_id = car_listings.id
            WHERE author_user_id = :authorUserId
              AND status::text = ANY(:statuses)
            ORDER BY created_at DESC
            LIMIT :size OFFSET :offset
            """;

    //language=postgresql
    private static final String SELECT_BY_ID_QUERY = """
            SELECT id, author_user_id, status::text, title, description, brand::text, custom_brand_name, model,
                   license_plate, condition::text, mileage, price, city::text, color::text, transmission::text,
                   fuel_type::text, tank_volume, drive_type::text, body_type::text, year, engine_volume,
                   owners_count, created_at, updated_at, published_at
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
                published_at      = :publishedAt
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
              AND status::text = ANY(:statuses)
            """;

    //language=postgresql
    private static final String SELECT_PUBLISHED_QUERY = """
            SELECT id, title, price, description, brand::text, custom_brand_name, model
            FROM car_listings
            WHERE status = 'PUBLISHED'
              AND published_at <= :publishedBefore
            ORDER BY published_at DESC, id DESC
            LIMIT :size OFFSET :offset
            """;

    //language=postgresql
    private static final String COUNT_PUBLISHED_QUERY = """
            SELECT COUNT(*)
            FROM car_listings
            WHERE status = 'PUBLISHED'
              AND published_at <= :publishedBefore
            """;

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
            SELECT cl.id, u.display_name, cl.title, cl.description, cl.brand::text, cl.custom_brand_name,
                   cl.model, cl.license_plate, cl.condition::text, cl.mileage, cl.price, cl.city::text,
                   cl.color::text, cl.transmission::text, cl.fuel_type::text, cl.tank_volume,
                   cl.drive_type::text, cl.body_type::text, cl.year, cl.engine_volume,
                   cl.owners_count, cl.published_at
            FROM car_listings cl
            JOIN users u ON cl.author_user_id = u.id
            WHERE cl.id = :id
              AND cl.status = 'PUBLISHED'
            """;

    private final DatabaseClient client;

    public CarListingRepository(DatabaseClient client) {
        this.client = client;
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
                        null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null,
                        null, null, null, null, null,
                        createdAt, createdAt, 0
                ))
                .one();
    }

    public Flux<OwnCarListingListItemDTO> findByUserIdAndStatuses(long authorUserId, String[] statuses, int page, int size) {
        return client.sql(SELECT_BY_USER_ID_AND_STATUSES_QUERY)
                .bind("authorUserId", authorUserId)
                .bind("statuses", statuses)
                .bind("size", size)
                .bind("offset", (long) page * size)
                .map(row -> new OwnCarListingListItemDTO(
                        row.get(0, Long.class),
                        ListingStatus.valueOf(row.get(1, String.class)),
                        row.get(2, String.class),
                        row.get(3, Long.class),
                        row.get(4, Long.class),
                        row.get(5, Long.class),
                        row.get(6, Long.class),
                        row.get(7, Long.class)
                ))
                .all();
    }

    public Mono<Long> countByUserIdAndStatuses(long authorUserId, String[] statuses) {
        return client.sql(COUNT_BY_USER_ID_AND_STATUSES_QUERY)
                .bind("authorUserId", authorUserId)
                .bind("statuses", statuses)
                .map(row -> row.get(0, Long.class))
                .one();
    }

    public Mono<CarListing> findById(long id) {
        return client.sql(SELECT_BY_ID_QUERY)
                .bind("id", id)
                .map(this::mapRow)
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
                .bind("publishedAt", carListing.publishedAt());

        spec = carListing.title() != null
                ? spec.bind("title", carListing.title())
                : spec.bindNull("title", String.class);
        spec = carListing.description() != null
                ? spec.bind("description", carListing.description())
                : spec.bindNull("description", String.class);
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
        return client.sql(SELECT_PUBLISHED_QUERY)
                .bind("publishedBefore", request.getPublishedBefore())
                .bind("size", request.getSize())
                .bind("offset", (long) request.getPage() * request.getSize())
                .map(row -> {
                    String brandStr = row.get(4, String.class);
                    return new PublicCarListingItemDTO(
                            row.get(0, Long.class),
                            row.get(1, String.class),
                            row.get(2, Long.class),
                            row.get(3, String.class),
                            brandStr != null ? CarBrand.valueOf(brandStr) : null,
                            row.get(5, String.class),
                            row.get(6, String.class)
                    );
                })
                .all();
    }

    public Mono<Long> countPublished(GetPublishedListingsRequestDTO request) {
        return client.sql(COUNT_PUBLISHED_QUERY)
                .bind("publishedBefore", request.getPublishedBefore())
                .map(row -> row.get(0, Long.class))
                .one();
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
                    String brandStr = row.get(4, String.class);
                    String conditionStr = row.get(8, String.class);
                    String cityStr = row.get(11, String.class);
                    String colorStr = row.get(12, String.class);
                    String transmissionStr = row.get(13, String.class);
                    String fuelTypeStr = row.get(14, String.class);
                    BigDecimal tankVol = row.get(15, BigDecimal.class);
                    String driveTypeStr = row.get(16, String.class);
                    String bodyTypeStr = row.get(17, String.class);
                    BigDecimal engVol = row.get(19, BigDecimal.class);
                    return new PublicCarListingDTO(
                            row.get(0, Long.class),
                            row.get(1, String.class),
                            row.get(2, String.class),
                            row.get(3, String.class),
                            brandStr != null ? CarBrand.valueOf(brandStr) : null,
                            row.get(5, String.class),
                            row.get(6, String.class),
                            row.get(7, String.class),
                            conditionStr != null ? CarCondition.valueOf(conditionStr) : null,
                            row.get(9, Integer.class),
                            row.get(10, Long.class),
                            cityStr != null ? City.valueOf(cityStr) : null,
                            colorStr != null ? CarColor.valueOf(colorStr) : null,
                            transmissionStr != null ? TransmissionType.valueOf(transmissionStr) : null,
                            fuelTypeStr != null ? FuelType.valueOf(fuelTypeStr) : null,
                            tankVol != null ? tankVol.doubleValue() : null,
                            driveTypeStr != null ? DriveType.valueOf(driveTypeStr) : null,
                            bodyTypeStr != null ? BodyType.valueOf(bodyTypeStr) : null,
                            row.get(18, Integer.class),
                            engVol != null ? engVol.doubleValue() : null,
                            row.get(20, Integer.class),
                            row.get(21, Long.class)
                    );
                })
                .one();
    }

    private CarListing mapRow(Readable row) {
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

        return new CarListing(
                row.get(0, Long.class),
                row.get(1, Long.class),
                ListingStatus.valueOf(row.get(2, String.class)),
                row.get(3, String.class),
                row.get(4, String.class),
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
                row.get(23, Long.class),
                row.get(24, Long.class)
        );
    }
}
