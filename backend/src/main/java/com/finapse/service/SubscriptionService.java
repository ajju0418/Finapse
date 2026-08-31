package com.finapse.service;

import com.finapse.dto.SubscriptionResponse;
import com.finapse.dto.SubscriptionSummaryResponse;
import com.finapse.entity.Transaction;
import com.finapse.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Turns the recurring-transaction data written during import into a subscription view.
 *
 * <p>The detection itself already happened in {@code RecurringTransactionDetector};
 * this only aggregates the resulting groups and derives cadence, price changes and
 * projected next charges.
 */
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    /** A charge is "late" past this multiple of its usual interval. */
    private static final double INACTIVE_INTERVAL_MULTIPLIER = 2.0;
    /** Ignore rounding noise when deciding a price went up. */
    private static final BigDecimal PRICE_INCREASE_THRESHOLD = new BigDecimal("1.00");
    private static final BigDecimal DAYS_PER_MONTH = new BigDecimal("30.44");

    private final TransactionRepository transactionRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public SubscriptionSummaryResponse getSubscriptions() {
        UUID userId = userService.getCurrentUserId();

        Map<String, List<Transaction>> groups = transactionRepository.findRecurringByUser(userId)
                .stream()
                .collect(Collectors.groupingBy(Transaction::getRecurringGroupId));

        List<SubscriptionResponse> subscriptions = groups.values().stream()
                .map(this::toSubscription)
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(SubscriptionResponse::amount).reversed())
                .toList();

        List<SubscriptionResponse> active = subscriptions.stream()
                .filter(s -> !s.possiblyInactive())
                .toList();

        BigDecimal monthlyTotal = active.stream()
                .map(this::toMonthlyEquivalent)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new SubscriptionSummaryResponse(
                subscriptions,
                active.size(),
                monthlyTotal,
                monthlyTotal.multiply(BigDecimal.valueOf(12)).setScale(2, RoundingMode.HALF_UP));
    }

    private SubscriptionResponse toSubscription(List<Transaction> group) {
        List<Transaction> ordered = group.stream()
                .filter(t -> t.getTransactionDate() != null)
                .sorted(Comparator.comparing(Transaction::getTransactionDate))
                .toList();

        if (ordered.isEmpty()) {
            return null;
        }

        Transaction latest = ordered.get(ordered.size() - 1);
        Transaction first = ordered.get(0);

        BigDecimal total = ordered.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = total.divide(BigDecimal.valueOf(ordered.size()), 2, RoundingMode.HALF_UP);

        long intervalDays = averageIntervalDays(ordered);
        LocalDate nextCharge = intervalDays > 0
                ? latest.getTransactionDate().plusDays(intervalDays)
                : null;

        boolean priceIncreased = ordered.size() >= 2
                && latest.getAmount()
                        .subtract(ordered.get(ordered.size() - 2).getAmount())
                        .compareTo(PRICE_INCREASE_THRESHOLD) > 0;

        boolean possiblyInactive = intervalDays > 0
                && ChronoUnit.DAYS.between(latest.getTransactionDate(), LocalDate.now())
                        > intervalDays * INACTIVE_INTERVAL_MULTIPLIER;

        return new SubscriptionResponse(
                latest.getRecurringGroupId(),
                latest.getMerchant() != null ? latest.getMerchant().getName() : "Unknown merchant",
                latest.getMerchant() != null ? latest.getMerchant().getId() : null,
                latest.getCategory() != null ? latest.getCategory().getDisplayName() : null,
                latest.getAmount(),
                average,
                ordered.size(),
                intervalDays,
                describeCadence(intervalDays),
                first.getTransactionDate(),
                latest.getTransactionDate(),
                nextCharge,
                total,
                priceIncreased,
                possiblyInactive);
    }

    private long averageIntervalDays(List<Transaction> ordered) {
        if (ordered.size() < 2) {
            return 0;
        }
        long totalDays = ChronoUnit.DAYS.between(
                ordered.get(0).getTransactionDate(),
                ordered.get(ordered.size() - 1).getTransactionDate());
        return Math.round((double) totalDays / (ordered.size() - 1));
    }

    private String describeCadence(long intervalDays) {
        if (intervalDays <= 0) return "Unknown";
        if (intervalDays <= 9) return "Weekly";
        if (intervalDays <= 20) return "Fortnightly";
        if (intervalDays <= 45) return "Monthly";
        if (intervalDays <= 100) return "Quarterly";
        if (intervalDays <= 200) return "Half-yearly";
        return "Annual";
    }

    /** Normalises any cadence to a comparable monthly cost. */
    private BigDecimal toMonthlyEquivalent(SubscriptionResponse subscription) {
        long interval = subscription.averageIntervalDays();
        if (interval <= 0) {
            return subscription.amount();
        }
        return subscription.amount()
                .multiply(DAYS_PER_MONTH)
                .divide(BigDecimal.valueOf(interval), 2, RoundingMode.HALF_UP);
    }
}
