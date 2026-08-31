package com.finapse.service;

import com.finapse.dto.CardAnalyticsResponse;
import com.finapse.entity.Card;
import com.finapse.entity.Transaction;
import com.finapse.entity.User;
import com.finapse.enums.TransactionType;
import com.finapse.repository.CardRepository;
import com.finapse.repository.StatementRepository;
import com.finapse.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceAnalyticsTest {

    @Mock CardRepository cardRepository;
    @Mock UserService userService;
    @Mock TransactionRepository transactionRepository;
    @Mock StatementRepository statementRepository;
    @InjectMocks CardService cardService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
    }

    @Test
    void analytics_reportsUtilisationAgainstTheCreditLimit() {
        Card card = card(new BigDecimal("100000.00"), null, null);
        stub(card, List.of(
                tx(TransactionType.EXPENSE, "40000.00", LocalDate.now()),
                tx(TransactionType.CREDIT_CARD_PAYMENT, "15000.00", LocalDate.now())));

        CardAnalyticsResponse analytics = cardService.getAnalytics(card.getId());

        assertThat(analytics.outstanding()).isEqualByComparingTo("25000.00");
        assertThat(analytics.utilizationPercent()).isEqualTo(25.0);
        assertThat(analytics.utilizationBand()).isEqualTo("LOW");
        assertThat(analytics.availableCredit()).isEqualByComparingTo("75000.00");
    }

    @Test
    void analytics_marksHighUtilisationAboveSeventyPercent() {
        Card card = card(new BigDecimal("10000.00"), null, null);
        stub(card, List.of(tx(TransactionType.EXPENSE, "8000.00", LocalDate.now())));

        assertThat(cardService.getAnalytics(card.getId()).utilizationBand()).isEqualTo("HIGH");
    }

    @Test
    void analytics_omitsUtilisationWhenNoCreditLimitIsSet() {
        Card card = card(null, null, null);
        stub(card, List.of(tx(TransactionType.EXPENSE, "8000.00", LocalDate.now())));

        CardAnalyticsResponse analytics = cardService.getAnalytics(card.getId());

        assertThat(analytics.utilizationPercent()).isNull();
        assertThat(analytics.utilizationBand()).isNull();
        assertThat(analytics.availableCredit()).isNull();
    }

    @Test
    void analytics_derivesACycleThatEndsOnOrAfterToday() {
        Card card = card(new BigDecimal("50000.00"), 5, 25);
        stub(card, List.of());

        CardAnalyticsResponse analytics = cardService.getAnalytics(card.getId());

        assertThat(analytics.currentCycleEnd()).isNotNull();
        assertThat(analytics.currentCycleStart()).isNotNull();
        assertThat(analytics.currentCycleEnd()).isAfterOrEqualTo(LocalDate.now());
        assertThat(analytics.currentCycleStart()).isBefore(analytics.currentCycleEnd());
        // The cycle spans roughly one month.
        assertThat(ChronoUnit.DAYS.between(analytics.currentCycleStart(), analytics.currentCycleEnd()))
                .isBetween(27L, 31L);
    }

    @Test
    void analytics_clampsACycleDayThatOverflowsShortMonths() {
        Card card = card(new BigDecimal("50000.00"), 31, 31);
        stub(card, List.of());

        CardAnalyticsResponse analytics = cardService.getAnalytics(card.getId());

        // Day 31 must land on the last valid day rather than throwing.
        assertThat(analytics.currentCycleEnd()).isNotNull();
        assertThat(analytics.nextDueDate()).isNotNull();
    }

    @Test
    void analytics_leavesCycleFieldsNullWhenNoBillingDayIsConfigured() {
        Card card = card(new BigDecimal("50000.00"), null, 20);
        stub(card, List.of(tx(TransactionType.EXPENSE, "500.00", LocalDate.now())));

        CardAnalyticsResponse analytics = cardService.getAnalytics(card.getId());

        assertThat(analytics.currentCycleStart()).isNull();
        assertThat(analytics.nextDueDate()).isNull();
        assertThat(analytics.currentCycleSpend()).isEqualByComparingTo("0");
    }

    @Test
    void analytics_countsOnlyThisCycleSpendInTheCycleTotal() {
        Card card = card(new BigDecimal("50000.00"), 5, 25);
        stub(card, List.of(
                tx(TransactionType.EXPENSE, "1000.00", LocalDate.now()),
                // Well before any current cycle window.
                tx(TransactionType.EXPENSE, "9000.00", LocalDate.now().minusMonths(6))));

        assertThat(cardService.getAnalytics(card.getId()).currentCycleSpend())
                .isEqualByComparingTo("1000.00");
    }

    // ---------------------------------------------------------------------

    private void stub(Card card, List<Transaction> transactions) {
        when(userService.getCurrentUserId()).thenReturn(user.getId());
        when(cardRepository.findByIdAndUserId(card.getId(), user.getId()))
                .thenReturn(Optional.of(card));
        when(transactionRepository.findByCardIdOrderByTransactionDateDesc(card.getId()))
                .thenReturn(transactions);
    }

    private Card card(BigDecimal limit, Integer cycleDay, Integer dueDay) {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setUser(user);
        card.setName("HDFC Regalia");
        card.setCreditLimit(limit);
        card.setBillingCycleDay(cycleDay);
        card.setPaymentDueDay(dueDay);
        return card;
    }

    private Transaction tx(TransactionType type, String amount, LocalDate date) {
        Transaction tx = new Transaction();
        tx.setId(UUID.randomUUID());
        tx.setTransactionType(type);
        tx.setAmount(new BigDecimal(amount));
        tx.setTransactionDate(date);
        return tx;
    }
}
