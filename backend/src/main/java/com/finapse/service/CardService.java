package com.finapse.service;

import com.finapse.dto.CardAnalyticsResponse;
import com.finapse.dto.CardCreateRequest;
import com.finapse.dto.CardResponse;
import com.finapse.entity.Card;
import com.finapse.entity.Transaction;
import com.finapse.enums.TransactionType;
import com.finapse.exception.ConflictException;
import com.finapse.exception.ResourceNotFoundException;
import com.finapse.repository.CardRepository;
import com.finapse.repository.StatementRepository;
import com.finapse.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserService userService;
    private final TransactionRepository transactionRepository;
    private final StatementRepository statementRepository;

    @Transactional(readOnly = true)
    public List<CardResponse> getAll() {
        UUID userId = userService.getCurrentUserId();
        return cardRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(CardResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CardResponse getById(UUID id) {
        return CardResponse.from(findOrThrow(id));
    }

    @Transactional
    public CardResponse create(CardCreateRequest request) {
        Card card = new Card();
        card.setUser(userService.getCurrentUser());
        card.setName(request.name());
        card.setIssuer(request.issuer());
        card.setLastFourDigits(request.lastFourDigits());
        card.setCreditLimit(request.creditLimit());
        card.setBillingCycleDay(request.billingCycleDay());
        card.setPaymentDueDay(request.paymentDueDay());
        if (request.statementPassword() != null && !request.statementPassword().isBlank()) {
            card.setStatementPassword(request.statementPassword());
        }
        return CardResponse.from(cardRepository.save(card));
    }

    @Transactional
    public CardResponse deactivate(UUID id) {
        Card card = findOrThrow(id);
        card.setActive(false);
        return CardResponse.from(cardRepository.save(card));
    }

    @Transactional
    public CardResponse update(UUID id, CardCreateRequest request) {
        Card card = findOrThrow(id);
        card.setName(request.name());
        card.setIssuer(request.issuer());
        card.setLastFourDigits(request.lastFourDigits());
        if (request.creditLimit() != null) card.setCreditLimit(request.creditLimit());
        if (request.billingCycleDay() != null) card.setBillingCycleDay(request.billingCycleDay());
        if (request.paymentDueDay() != null) card.setPaymentDueDay(request.paymentDueDay());
        if (request.statementPassword() != null) {
            card.setStatementPassword(request.statementPassword().isBlank() ? null : request.statementPassword());
        }
        return CardResponse.from(cardRepository.save(card));
    }

    @Transactional
    public void delete(UUID id) {
        Card card = findOrThrow(id);
        if (statementRepository.existsByCardId(id)) {
            throw new ConflictException("Cannot delete card because it has linked statements. Delete the statements first, or deactivate the card.");
        }
        cardRepository.delete(card);
    }

    @Transactional(readOnly = true)
    public CardAnalyticsResponse getAnalytics(UUID cardId) {
        Card card = findOrThrow(cardId);
        List<Transaction> txs = transactionRepository.findByCardIdOrderByTransactionDateDesc(cardId);

        BigDecimal spending = sum(txs, TransactionType.EXPENSE);
        BigDecimal cashback = sum(txs, TransactionType.CASHBACK);
        BigDecimal payments = sum(txs, TransactionType.CREDIT_CARD_PAYMENT);
        // Outstanding = spending - payments received toward this card
        BigDecimal outstanding = spending.subtract(payments).max(BigDecimal.ZERO);
        BigDecimal limit = card.getCreditLimit();
        BigDecimal available = limit != null
                ? limit.subtract(outstanding).max(BigDecimal.ZERO)
                : null;

        Double utilization = null;
        String band = null;
        if (limit != null && limit.compareTo(BigDecimal.ZERO) > 0) {
            utilization = outstanding.multiply(BigDecimal.valueOf(100))
                    .divide(limit, 1, RoundingMode.HALF_UP)
                    .doubleValue();
            band = utilization < 30 ? "LOW" : utilization <= 70 ? "MODERATE" : "HIGH";
        }

        BillingCycle cycle = resolveCycle(card);
        BigDecimal cycleSpend = BigDecimal.ZERO;
        if (cycle != null) {
            for (Transaction tx : txs) {
                if (tx.getTransactionType() != TransactionType.EXPENSE) continue;
                LocalDate date = tx.getTransactionDate();
                if (!date.isBefore(cycle.start()) && !date.isAfter(cycle.end())) {
                    cycleSpend = cycleSpend.add(tx.getAmount());
                }
            }
        }

        return new CardAnalyticsResponse(
                card.getId(), card.getName(),
                spending, cashback, payments,
                outstanding, available,
                (int) txs.stream().filter(t -> t.getTransactionType() == TransactionType.EXPENSE).count(),
                limit, utilization, band,
                cycle != null ? cycle.start() : null,
                cycle != null ? cycle.end() : null,
                cycleSpend,
                cycle != null ? cycle.statementDate() : null,
                cycle != null ? cycle.dueDate() : null,
                cycle != null && cycle.dueDate() != null
                        ? (int) ChronoUnit.DAYS.between(LocalDate.now(), cycle.dueDate())
                        : null
        );
    }

    private record BillingCycle(LocalDate start, LocalDate end, LocalDate statementDate, LocalDate dueDate) {}

    /**
     * Derives the current statement window from the configured billing-cycle day.
     * The cycle closes on {@code billingCycleDay} and the bill falls due on
     * {@code paymentDueDay} of the following month (or the same month when the due
     * day is later than the cycle day).
     */
    private BillingCycle resolveCycle(Card card) {
        Integer cycleDay = card.getBillingCycleDay();
        if (cycleDay == null || cycleDay < 1 || cycleDay > 31) return null;

        LocalDate today = LocalDate.now();
        LocalDate thisMonthClose = atDayOfMonth(YearMonth.from(today), cycleDay);

        LocalDate cycleEnd = today.isAfter(thisMonthClose)
                ? atDayOfMonth(YearMonth.from(today).plusMonths(1), cycleDay)
                : thisMonthClose;
        LocalDate cycleStart = atDayOfMonth(YearMonth.from(cycleEnd).minusMonths(1), cycleDay).plusDays(1);

        LocalDate dueDate = null;
        Integer dueDay = card.getPaymentDueDay();
        if (dueDay != null && dueDay >= 1 && dueDay <= 31) {
            YearMonth dueMonth = dueDay > cycleDay ? YearMonth.from(cycleEnd) : YearMonth.from(cycleEnd).plusMonths(1);
            dueDate = atDayOfMonth(dueMonth, dueDay);
        }
        return new BillingCycle(cycleStart, cycleEnd, cycleEnd, dueDate);
    }

    /** Clamps the requested day to the month's length so Feb 30 becomes Feb 28/29. */
    private LocalDate atDayOfMonth(YearMonth month, int day) {
        return month.atDay(Math.min(day, month.lengthOfMonth()));
    }

    private BigDecimal sum(List<Transaction> txs, TransactionType type) {
        return txs.stream()
                .filter(t -> t.getTransactionType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Card findOrThrow(UUID id) {
        return cardRepository.findByIdAndUserId(id, userService.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Card not found: " + id));
    }
}
