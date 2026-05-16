package edu.automarket.favourites;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class FavouritesRepository {
    //language=postgresql
    private static final String INSERT_FAVOURITE = """
            INSERT INTO favourites (user_id, listing_id, created_at)
            VALUES (:userId, :listingId, EXTRACT(EPOCH FROM NOW())::bigint * 1000)
            """;

    //language=postgresql
    private static final String DELETE_FAVOURITE = """
            DELETE FROM favourites
            WHERE user_id = :userId AND listing_id = :listingId
            """;

    //language=postgresql
    private static final String COUNT_FAVOURITES_BY_USER = """
            SELECT COUNT(*)
            FROM favourites
            WHERE user_id = :userId
            """;

    private final DatabaseClient databaseClient;

    public FavouritesRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    public Mono<Void> addFavourite(long userId, long listingId) {
        return databaseClient.sql(INSERT_FAVOURITE)
                .bind("userId", userId)
                .bind("listingId", listingId)
                .then();
    }

    public Mono<Void> removeFavourite(long userId, long listingId) {
        return databaseClient.sql(DELETE_FAVOURITE)
                .bind("userId", userId)
                .bind("listingId", listingId)
                .then();
    }

    public Mono<Integer> countFavourites(long userId) {
        return databaseClient.sql(COUNT_FAVOURITES_BY_USER)
                .bind("userId", userId)
                .map(row -> row.get(0, Integer.class))
                .one();
    }
}
