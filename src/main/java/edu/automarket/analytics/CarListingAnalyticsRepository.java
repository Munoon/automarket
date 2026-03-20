package edu.automarket.analytics;

import edu.automarket.analytics.dto.ListingAnalyticsDayDTO;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map.Entry;

@Repository
public class CarListingAnalyticsRepository {
    //language=postgresql
    private static final String UPSERT_QUERY = """
            INSERT INTO car_listing_analytics (
                listing_id, impressions_count, views_count, phone_requests_count, favourites_count, ts
            ) VALUES ($1, $2, $3, $4, $5, $6)
            ON CONFLICT (listing_id, ts) DO UPDATE
                SET impressions_count    = car_listing_analytics.impressions_count + EXCLUDED.impressions_count,
                    views_count          = car_listing_analytics.views_count + EXCLUDED.views_count,
                    phone_requests_count = car_listing_analytics.phone_requests_count + EXCLUDED.phone_requests_count,
                    favourites_count     = car_listing_analytics.favourites_count + EXCLUDED.favourites_count
            """;

    //language=postgresql
    private static final String SELECT_ANALYTICS_BY_DAY_QUERY = """
            SELECT EXTRACT(EPOCH FROM date_trunc('day', ts AT TIME ZONE 'UTC', :zoneId))::BIGINT AS day,
                   sum(impressions_count), sum(views_count), sum(phone_requests_count), sum(favourites_count)
            FROM car_listing_analytics
            WHERE listing_id = :listingId
              AND ts >= :minTS
            GROUP BY day
            ORDER BY day ASC
            """;

    //language=postgresql
    private static final String CLEAR_OLD_ANALYTICS = """
            DELETE FROM car_listing_analytics
            WHERE ts < :ts
            """;

    private final DatabaseClient client;

    public CarListingAnalyticsRepository(DatabaseClient client) {
        this.client = client;
    }

    public Mono<Void> saveAnalytics(long countersTS, List<Entry<Long, CarListingAnalyticsCounter>> analytics) {
        Instant truncatedInstant = Instant.ofEpochMilli(countersTS - countersTS % 3600000L);
        LocalDateTime ts = LocalDateTime.ofInstant(truncatedInstant, ZoneOffset.UTC);
        return client.inConnection(connection -> {
            Statement statement = connection.createStatement(UPSERT_QUERY);
            for (int i = 0; i < analytics.size(); i++) {
                Entry<Long, CarListingAnalyticsCounter> entry = analytics.get(i);
                CarListingAnalyticsCounter counter = entry.getValue();

                if (i > 0) {
                    statement.add();
                }
                statement
                        .bind(0, entry.getKey())
                        .bind(1, counter.impressionsCount().sum())
                        .bind(2, counter.viewsCount().sum())
                        .bind(3, counter.phoneRequestsCount().sum())
                        .bind(4, counter.favouritesCount().sum())
                        .bind(5, ts);
            }

            return Flux.from(statement.execute()).flatMap(Result::getRowsUpdated).then();
        });
    }

    public Flux<ListingAnalyticsDayDTO> getAnalyticsByDay(long listingId, ZoneId zoneId, long minTS) {
        return client.sql(SELECT_ANALYTICS_BY_DAY_QUERY)
                .bind("listingId", listingId)
                .bind("zoneId", zoneId.getId())
                .bind("minTS", LocalDateTime.ofInstant(Instant.ofEpochMilli(minTS), ZoneOffset.UTC))
                .map(row -> new ListingAnalyticsDayDTO(
                        row.get(0, Long.class),
                        row.get(1, Long.class),
                        row.get(2, Long.class),
                        row.get(3, Long.class),
                        row.get(4, Long.class)
                ))
                .all();
    }

    public Mono<Long> clearOldAnalytics(long minTS) {
        LocalDateTime ts = LocalDateTime.ofInstant(Instant.ofEpochMilli(minTS), ZoneOffset.UTC);
        return client.sql(CLEAR_OLD_ANALYTICS)
                .bind("ts", ts)
                .fetch().rowsUpdated();
    }
}
