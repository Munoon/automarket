package edu.automarket.listing;

import edu.automarket.listing.dto.OwnCarListingListItemDTO;
import edu.automarket.listing.dto.PublicCarListingItemDTO;
import edu.automarket.listing.dto.UpdateCarListingRequestDTO;
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
            SELECT id, status::text, title, published_at
            FROM car_listings
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
            SET title             = :title,
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
                updated_at        = :updatedAt
            WHERE id = :id
            """;

    //language=postgresql
    private static final String DELETE_BY_ID_QUERY = """
            DELETE FROM car_listings WHERE id = :id
            """;

    //language=postgresql
    private static final String UPDATE_STATUS_QUERY = """
            UPDATE car_listings
            SET status       = :status::listing_status,
                published_at = :publishedAt,
                updated_at   = :updatedAt
            WHERE id = :id
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
                        row.get(3, Long.class)
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

    public Mono<Void> updateStatus(long id, ListingStatus status, long publishedAt) {
        return client.sql(UPDATE_STATUS_QUERY)
                .bind("id", id)
                .bind("status", status.name())
                .bind("updatedAt", System.currentTimeMillis())
                .bind("publishedAt", publishedAt)
                .fetch().rowsUpdated().then();
    }

    public Mono<Void> update(long id, UpdateCarListingRequestDTO request) {
        var spec = client.sql(UPDATE_QUERY)
                .bind("id", id)
                .bind("updatedAt", System.currentTimeMillis());

        spec = request.title() != null
                ? spec.bind("title", request.title())
                : spec.bindNull("title", String.class);
        spec = request.description() != null
                ? spec.bind("description", request.description())
                : spec.bindNull("description", String.class);
        spec = request.brand() != null
                ? spec.bind("brand", request.brand().name())
                : spec.bindNull("brand", String.class);
        spec = request.customBrandName() != null
                ? spec.bind("customBrandName", request.customBrandName())
                : spec.bindNull("customBrandName", String.class);
        spec = request.model() != null
                ? spec.bind("model", request.model())
                : spec.bindNull("model", String.class);
        spec = request.licensePlate() != null
                ? spec.bind("licensePlate", request.licensePlate())
                : spec.bindNull("licensePlate", String.class);
        spec = request.condition() != null
                ? spec.bind("condition", request.condition().name())
                : spec.bindNull("condition", String.class);
        spec = request.mileage() != null
                ? spec.bind("mileage", request.mileage())
                : spec.bindNull("mileage", Integer.class);
        spec = request.price() != null
                ? spec.bind("price", request.price())
                : spec.bindNull("price", Long.class);
        spec = request.city() != null
                ? spec.bind("city", request.city().name())
                : spec.bindNull("city", String.class);
        spec = request.color() != null
                ? spec.bind("color", request.color().name())
                : spec.bindNull("color", String.class);
        spec = request.transmission() != null
                ? spec.bind("transmission", request.transmission().name())
                : spec.bindNull("transmission", String.class);
        spec = request.fuelType() != null
                ? spec.bind("fuelType", request.fuelType().name())
                : spec.bindNull("fuelType", String.class);
        spec = request.tankVolume() != null
                ? spec.bind("tankVolume", request.tankVolume())
                : spec.bindNull("tankVolume", Double.class);
        spec = request.driveType() != null
                ? spec.bind("driveType", request.driveType().name())
                : spec.bindNull("driveType", String.class);
        spec = request.bodyType() != null
                ? spec.bind("bodyType", request.bodyType().name())
                : spec.bindNull("bodyType", String.class);
        spec = request.year() != null
                ? spec.bind("year", request.year())
                : spec.bindNull("year", Integer.class);
        spec = request.engineVolume() != null
                ? spec.bind("engineVolume", request.engineVolume())
                : spec.bindNull("engineVolume", Double.class);
        spec = request.ownersCount() != null
                ? spec.bind("ownersCount", request.ownersCount())
                : spec.bindNull("ownersCount", Integer.class);

        return spec.fetch().rowsUpdated().then();
    }

    public Flux<PublicCarListingItemDTO> findPublished(long publishedBefore, int page, int size) {
        return client.sql(SELECT_PUBLISHED_QUERY)
                .bind("publishedBefore", publishedBefore)
                .bind("size", size)
                .bind("offset", (long) page * size)
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

    public Mono<Long> countPublished(long publishedBefore) {
        return client.sql(COUNT_PUBLISHED_QUERY)
                .bind("publishedBefore", publishedBefore)
                .map(row -> row.get(0, Long.class))
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
