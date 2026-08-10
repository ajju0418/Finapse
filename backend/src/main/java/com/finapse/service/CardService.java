package com.finapse.service;

import com.finapse.dto.CardAnalyticsResponse;
import com.finapse.dto.CardCreateRequest;
import com.finapse.dto.CardResponse;
import com.finapse.entity.Card;
import com.finapse.entity.Transaction;
import com.finapse.enums.TransactionType;
import com.finapse.exception.ResourceNotFoundException;
import com.finapse.repository.CardRepository;
import com.finapse.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserService userService;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<CardResponse> getAll() {
        UUID userId = userService.getDefaultUser().getId();
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
        card.setUser(userService.getDefaultUser());
        card.setName(request.name());
        card.setIssuer(request.issuer());
        card.setLastFourDigits(request.lastFourDigits());
        card.setCreditLimit(request.creditLimit());
        card.setBillingCycleDay(request.billingCycleDay());
        card.setPaymentDueDay(request.paymentDueDay());
        return CardResponse.from(cardRepository.save(card));
    }

    @Transactional
    public CardResponse deactivate(UUID id) {
        Card card = findOrThrow(id);
        card.setActive(false);
        return CardResponse.from(cardRepository.save(card));
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
        BigDecimal available = card.getCreditLimit() != null
                ? card.getCreditLimit().subtract(outstanding).max(BigDecimal.ZERO)
                : null;

        return new CardAnalyticsResponse(
                card.getId(), card.getName(),
                spending, cashback, payments,
                outstanding, available,
                (int) txs.stream().filter(t -> t.getTransactionType() == TransactionType.EXPENSE).count()
        );
    }

    private BigDecimal sum(List<Transaction> txs, TransactionType type) {
        return txs.stream()
                .filter(t -> t.getTransactionType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Card findOrThrow(UUID id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Card not found: " + id));
    }
}
