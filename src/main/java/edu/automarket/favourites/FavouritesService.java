package edu.automarket.favourites;

import edu.automarket.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class FavouritesService {
    private final FavouritesRepository repository;
    private final int favouritesLimitPerUser;

    public FavouritesService(FavouritesRepository repository,
                             @Value("${app.favourites.limitPerUser:100}") int favouritesLimitPerUser) {
        this.repository = repository;
        this.favouritesLimitPerUser = favouritesLimitPerUser;
    }

    public Mono<Void> addFavourite(long userId, long listingId) {
        return countFavouritesByUser(userId)
                .flatMap(count -> {
                    if (count >= favouritesLimitPerUser) {
                        throw new ApiException(HttpStatus.FORBIDDEN, "/problems/favourites-limit-reached",
                                "You have reached the limit of favourites");
                    }
                    return repository.addFavourite(userId, listingId);
                });
    }

    public Mono<Integer> countFavouritesByUser(long userId) {
        return repository.countFavourites(userId);
    }

    public Mono<Void> removeFavourite(long userId, long listingId) {
        return repository.removeFavourite(userId, listingId);
    }

    public Mono<Void> removeFavouritesByListingId(long listingId) {
        return repository.removeFavouritesByListingId(listingId);
    }

    public int favouritesLimitPerUser() {
        return favouritesLimitPerUser;
    }
}
