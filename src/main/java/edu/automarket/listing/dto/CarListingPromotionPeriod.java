package edu.automarket.listing.dto;

public enum CarListingPromotionPeriod {
    ONE_WEEK(7L * 24 * 60 * 60 * 1000),
    TWO_WEEKS(14L * 24 * 60 * 60 * 1000),
    ONE_MONTH(30L * 24 * 60 * 60 * 1000);

    public final long ms;

    CarListingPromotionPeriod(long ms) {
        this.ms = ms;
    }
}
