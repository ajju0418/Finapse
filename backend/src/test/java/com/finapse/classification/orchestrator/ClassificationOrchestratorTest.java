package com.finapse.classification.orchestrator;

import com.finapse.classification.detection.MerchantDetectionService;
import com.finapse.classification.detection.NormalizationService;
import com.finapse.classification.detection.RecurringTransactionDetector;
import com.finapse.classification.strategy.ClassificationStrategy;
import com.finapse.classification.strategy.ExactMerchantClassifier;
import com.finapse.classification.strategy.RuleBasedClassifier;
import com.finapse.dto.RawTransactionRecord;
import com.finapse.entity.Account;
import com.finapse.entity.Category;
import com.finapse.entity.Merchant;
import com.finapse.entity.Statement;
import com.finapse.entity.Transaction;
import com.finapse.enums.TransactionDirection;
import com.finapse.enums.TransactionType;
import com.finapse.repository.MerchantRepository;
import com.finapse.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassificationOrchestratorTest {

    private ClassificationOrchestrator orchestrator;

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        NormalizationService normalizationService = new NormalizationService();
        MerchantDetectionService merchantDetectionService = new MerchantDetectionService(merchantRepository);
        RecurringTransactionDetector recurringDetector = new RecurringTransactionDetector(transactionRepository);

        List<ClassificationStrategy> strategies = List.of(
                new ExactMerchantClassifier(),
                new RuleBasedClassifier()
        );

        orchestrator = new ClassificationOrchestrator(
                normalizationService,
                merchantDetectionService,
                recurringDetector,
                strategies
        );
    }

    @Test
    void testExactMerchantMatch() {
        Category foodCategory = new Category();
        foodCategory.setName("FOOD_AND_DINING");

        Merchant zomato = new Merchant();
        zomato.setNormalizedName("ZOMATO");
        zomato.setCategory(foodCategory);

        when(merchantRepository.findByNormalizedName("ZOMATO")).thenReturn(Optional.of(zomato));

        RawTransactionRecord raw = new RawTransactionRecord(
                1,
                LocalDate.now(),
                LocalDate.now(),
                "UPI-ZOMATO-GPAY-12203264768",
                new BigDecimal("500.00"),
                TransactionDirection.DEBIT,
                "500.00",
                "",
                ""
        );
        Statement stmt = new Statement();
        Account acc = new Account();
        acc.setId(UUID.randomUUID());

        Transaction tx = orchestrator.orchestrate(raw, stmt, acc, null);

        assertEquals(TransactionType.EXPENSE, tx.getTransactionType());
        assertEquals("FOOD_AND_DINING", tx.getCategory().getName());
        assertEquals("ZOMATO", tx.getMerchant().getNormalizedName());
    }

    @Test
    void testSalaryIncomeMatch() {
        when(merchantRepository.findByNormalizedName(anyString())).thenReturn(Optional.empty());
        when(merchantRepository.findByNarrationContaining(anyString())).thenReturn(Collections.emptyList());

        RawTransactionRecord raw = new RawTransactionRecord(
                1,
                LocalDate.now(),
                LocalDate.now(),
                "NEFT CR-COGNIZANT SAL",
                new BigDecimal("50000.00"),
                TransactionDirection.CREDIT,
                "",
                "50000.00",
                ""
        );
        Statement stmt = new Statement();
        Account acc = new Account();
        acc.setId(UUID.randomUUID());

        Transaction tx = orchestrator.orchestrate(raw, stmt, acc, null);

        assertEquals(TransactionType.INCOME, tx.getTransactionType());
    }

    @Test
    void testGenericUpiTransfer() {
        when(merchantRepository.findByNormalizedName(anyString())).thenReturn(Optional.empty());
        when(merchantRepository.findByNarrationContaining(anyString())).thenReturn(Collections.emptyList());

        RawTransactionRecord raw = new RawTransactionRecord(
                1,
                LocalDate.now(),
                LocalDate.now(),
                "UPI-VARSHINI ASHOKKUMAR-GPAY-12203264768",
                new BigDecimal("500.00"),
                TransactionDirection.DEBIT,
                "500.00",
                "",
                ""
        );
        Statement stmt = new Statement();
        Account acc = new Account();
        acc.setId(UUID.randomUUID());

        Transaction tx = orchestrator.orchestrate(raw, stmt, acc, null);

        assertEquals(TransactionType.EXPENSE, tx.getTransactionType());
    }
}
