package com.finapse.dto;

import java.math.BigDecimal;
import java.util.List;

/** Everything the Subscriptions screen needs in a single round trip. */
public record SubscriptionSummaryResponse(
        List<SubscriptionResponse> subscriptions,
        int activeCount,
        /** Sum of every active subscription normalised to a monthly figure. */
        BigDecimal monthlyTotal,
        /** {@code monthlyTotal} projected over a year. */
        BigDecimal annualisedTotal
) {}
