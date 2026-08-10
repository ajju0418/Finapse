package com.finapse.service;

import com.finapse.dto.CardCreateRequest;
import com.finapse.dto.CardResponse;
import com.finapse.entity.Card;
import com.finapse.entity.User;
import com.finapse.exception.ResourceNotFoundException;
import com.finapse.repository.CardRepository;
import com.finapse.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock CardRepository cardRepository;
    @Mock UserService userService;
    @Mock TransactionRepository transactionRepository;
    @InjectMocks CardService cardService;

    private User defaultUser;

    @BeforeEach
    void setUp() {
        defaultUser = new User();
        defaultUser.setId(UUID.randomUUID());
        defaultUser.setName("Local User");
    }

    @Test
    void create_persistsCardWithCorrectFields() {
        when(userService.getDefaultUser()).thenReturn(defaultUser);
        CardCreateRequest request = new CardCreateRequest(
                "SBI Cashback", "SBI", "4821",
                new BigDecimal("100000.00"), 5, 15);

        Card saved = new Card();
        saved.setId(UUID.randomUUID());
        saved.setUser(defaultUser);
        saved.setName(request.name());
        saved.setIssuer(request.issuer());
        saved.setLastFourDigits(request.lastFourDigits());
        saved.setCreditLimit(request.creditLimit());
        saved.setBillingCycleDay(request.billingCycleDay());
        saved.setPaymentDueDay(request.paymentDueDay());

        when(cardRepository.save(any(Card.class))).thenReturn(saved);

        CardResponse response = cardService.create(request);

        assertThat(response.name()).isEqualTo("SBI Cashback");
        assertThat(response.issuer()).isEqualTo("SBI");
        assertThat(response.lastFourDigits()).isEqualTo("4821");
        assertThat(response.creditLimit()).isEqualByComparingTo("100000.00");
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void getAll_returnsCardsForDefaultUser() {
        when(userService.getDefaultUser()).thenReturn(defaultUser);
        Card c = new Card();
        c.setId(UUID.randomUUID());
        c.setUser(defaultUser);
        c.setName("HDFC Regalia");

        when(cardRepository.findByUserIdOrderByCreatedAtDesc(defaultUser.getId()))
                .thenReturn(List.of(c));

        List<CardResponse> result = cardService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("HDFC Regalia");
    }

    @Test
    void getById_throwsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(cardRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deactivate_setsIsActiveFalse() {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setUser(defaultUser);
        card.setName("Old Card");
        card.setActive(true);

        when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));
        when(cardRepository.save(card)).thenReturn(card);

        CardResponse response = cardService.deactivate(card.getId());

        assertThat(response.isActive()).isFalse();
    }
}
