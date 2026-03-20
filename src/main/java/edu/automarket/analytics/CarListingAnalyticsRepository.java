package edu.automarket.analytics;

import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
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
    private static final String CLEAR_OLD_ANALYTICS = """
            DELETE FROM car_listing_analytics
            WHERE ts < :ts
            """;

    private final DatabaseClient client;

    public CarListingAnalyticsRepository(DatabaseClient client) {
        this.client = client;
    }

    public Mono<Void> saveAnalytics(long countersTS, List<Entry<Long, CarListingAnalyticsCounter>> analytics) {
        LocalDateTime ts = LocalDateTime.ofInstant(Instant.ofEpochMilli(countersTS), ZoneOffset.UTC);
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

    public Mono<Long> clearOldAnalytics(long minTS) {
        LocalDateTime ts = LocalDateTime.ofInstant(Instant.ofEpochMilli(minTS), ZoneOffset.UTC);
        return client.sql(CLEAR_OLD_ANALYTICS)
                .bind("ts", ts)
                .fetch().rowsUpdated();
    }
}
