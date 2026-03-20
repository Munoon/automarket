package edu.automarket.analytics;

import java.util.concurrent.atomic.LongAdder;

public record CarListingAnalyticsCounter(
        LongAdder impressionsCount,
        LongAdder viewsCount,
        LongAdder phoneRequestsCount,
        LongAdder favouritesCount
) {
    public CarListingAnalyticsCounter() {
        this(new LongAdder(), new LongAdder(), new LongAdder(), new LongAdder());
    }
}
