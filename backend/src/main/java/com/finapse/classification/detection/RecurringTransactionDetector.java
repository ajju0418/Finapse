package com.finapse.classification.detection;

import com.finapse.entity.Transaction;
import com.finapse.enums.TransactionType;
import com.finapse.repository.TransactionRepository;
import com.finapse.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringTransactionDetector {

    private static final int MIN_OCCURRENCES = 2;
    private static final double AMOUNT_TOLERANCE_PERCENT = 5.0;
    private static final long MAX_INTERVAL_DAYS = 35;
    private static final long MIN_INTERVAL_DAYS = 25;

    private final TransactionRepository transactionRepository;

    @Transactional
    public List<RecurringGroup> detectRecurringPatterns(UUID userId, LocalDate from, LocalDate to) {
        List<Transaction> transactions = transactionRepository.findByUserAndDateRange(userId, from, to);
        return findRecurringGroups(transactions);
    }

    public RecurringCheckResult checkIfRecurring(Transaction newTransaction, UUID userId) {
        if (newTransaction.getMerchant() == null) {
            return RecurringCheckResult.notRecurring();
        }

        List<Transaction> history = transactionRepository.findByUserMerchantAndAmount(
                userId,
                newTransaction.getMerchant().getId(),
                newTransaction.getAmount());

        if (history.size() < MIN_OCCURRENCES) {
            List<Transaction> similarAmount = transactionRepository.findByUserAndMerchant(
                    userId, newTransaction.getMerchant().getId());
            history = similarAmount.stream()
                    .filter(t -> isAmountSimilar(t.getAmount(), newTransaction.getAmount()))
                    .collect(Collectors.toList());
        }

        if (history.size() < MIN_OCCURRENCES) {
            return RecurringCheckResult.notRecurring();
        }

        List<LocalDate> dates = history.stream()
                .map(Transaction::getTransactionDate)
                .sorted()
                .collect(Collectors.toList());

        if (newTransaction.getTransactionDate() != null) {
            dates.add(newTransaction.getTransactionDate());
            dates.sort(Comparator.naturalOrder());
        }

        IntervalAnalysis analysis = analyzeIntervals(dates);
        if (!analysis.isRegular) {
            return RecurringCheckResult.notRecurring();
        }

        String groupId = buildRecurringGroupId(newTransaction);
        TransactionType suggestedType = inferRecurringType(newTransaction, analysis);
        double confidence = computeRecurringConfidence(history.size(), analysis);

        return new RecurringCheckResult(true, groupId, suggestedType, confidence, analysis.averageIntervalDays);
    }

    private List<RecurringGroup> findRecurringGroups(List<Transaction> transactions) {
        Map<String, List<Transaction>> byMerchant = transactions.stream()
                .filter(t -> t.getMerchant() != null)
                .collect(Collectors.groupingBy(t -> t.getMerchant().getId().toString()));

        List<RecurringGroup> groups = new ArrayList<>();

        for (var entry : byMerchant.entrySet()) {
            List<Transaction> merchantTxns = entry.getValue();
            Map<String, List<Transaction>> byAmountBucket = groupByAmountBucket(merchantTxns);

            for (var amountGroup : byAmountBucket.entrySet()) {
                List<Transaction> group = amountGroup.getValue();
                if (group.size() < MIN_OCCURRENCES) continue;

                List<LocalDate> dates = group.stream()
                        .map(Transaction::getTransactionDate)
                        .sorted()
                        .collect(Collectors.toList());

                IntervalAnalysis analysis = analyzeIntervals(dates);
                if (analysis.isRegular) {
                    String groupId = HashUtil.sha256(entry.getKey() + "|" + amountGroup.getKey());
                    groups.add(new RecurringGroup(
                            groupId,
                            group,
                            analysis.averageIntervalDays,
                            inferRecurringType(group.get(0), analysis)));
                }
            }
        }

        return groups;
    }

    private Map<String, List<Transaction>> groupByAmountBucket(List<Transaction> transactions) {
        Map<String, List<Transaction>> buckets = new HashMap<>();
        for (Transaction tx : transactions) {
            BigDecimal rounded = tx.getAmount().setScale(0, RoundingMode.HALF_UP);
            String key = rounded.toPlainString();
            buckets.computeIfAbsent(key, k -> new ArrayList<>()).add(tx);
        }
        return buckets;
    }

    private IntervalAnalysis analyzeIntervals(List<LocalDate> sortedDates) {
        if (sortedDates.size() < 2) return new IntervalAnalysis(false, 0, 0);

        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < sortedDates.size(); i++) {
            long days = ChronoUnit.DAYS.between(sortedDates.get(i - 1), sortedDates.get(i));
            if (days > 0) intervals.add(days);
        }

        if (intervals.isEmpty()) return new IntervalAnalysis(false, 0, 0);

        double avg = intervals.stream().mapToLong(Long::longValue).average().orElse(0);
        double stdDev = Math.sqrt(intervals.stream()
                .mapToDouble(d -> Math.pow(d - avg, 2))
                .average()
                .orElse(0));

        boolean isRegular = avg >= MIN_INTERVAL_DAYS
                && avg <= MAX_INTERVAL_DAYS
                && stdDev <= 7.0;

        boolean isWeekly = avg >= 5 && avg <= 9 && stdDev <= 2.0;
        boolean isBiweekly = avg >= 12 && avg <= 16 && stdDev <= 3.0;

        return new IntervalAnalysis(isRegular || isWeekly || isBiweekly, avg, stdDev);
    }

    private boolean isAmountSimilar(BigDecimal a, BigDecimal b) {
        if (a.compareTo(BigDecimal.ZERO) == 0 || b.compareTo(BigDecimal.ZERO) == 0) return false;
        BigDecimal diff = a.subtract(b).abs();
        BigDecimal percentDiff = diff.divide(a.max(b), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        return percentDiff.compareTo(BigDecimal.valueOf(AMOUNT_TOLERANCE_PERCENT)) <= 0;
    }

    private TransactionType inferRecurringType(Transaction tx, IntervalAnalysis analysis) {
        String desc = tx.getDescription() != null ? tx.getDescription().toUpperCase() : "";

        if (desc.contains("EMI") || desc.contains("INSTALMENT") || desc.contains("INSTALLMENT")) {
            return TransactionType.EMI;
        }
        if (desc.contains("SIP") || desc.contains("MUTUAL FUND")) {
            return TransactionType.TRANSFER;
        }
        if (desc.contains("INSURANCE") || desc.contains("PREMIUM")) {
            return TransactionType.EXPENSE;
        }
        if (desc.contains("RENT") || desc.contains("LEASE")) {
            return TransactionType.EXPENSE;
        }

        if (analysis.averageIntervalDays >= 25 && analysis.averageIntervalDays <= 35) {
            return TransactionType.SUBSCRIPTION;
        }
        return TransactionType.SUBSCRIPTION;
    }

    private double computeRecurringConfidence(int occurrences, IntervalAnalysis analysis) {
        double base = 0.65;
        double countBonus = Math.min(occurrences * 0.05, 0.20);
        double regularityBonus = analysis.stdDev < 3.0 ? 0.10 : (analysis.stdDev < 5.0 ? 0.05 : 0);
        return Math.min(base + countBonus + regularityBonus, 0.95);
    }

    private String buildRecurringGroupId(Transaction tx) {
        String merchantId = tx.getMerchant() != null ? tx.getMerchant().getId().toString() : "unknown";
        String amountKey = tx.getAmount().setScale(0, RoundingMode.HALF_UP).toPlainString();
        return HashUtil.sha256(merchantId + "|" + amountKey);
    }

    public record IntervalAnalysis(boolean isRegular, double averageIntervalDays, double stdDev) {}

    public record RecurringGroup(String groupId, List<Transaction> transactions,
                                 double averageIntervalDays, TransactionType suggestedType) {}

    public record RecurringCheckResult(boolean isRecurring, String groupId,
                                       TransactionType suggestedType, double confidence,
                                       double intervalDays) {
        public static RecurringCheckResult notRecurring() {
            return new RecurringCheckResult(false, null, null, 0, 0);
        }
    }
}
