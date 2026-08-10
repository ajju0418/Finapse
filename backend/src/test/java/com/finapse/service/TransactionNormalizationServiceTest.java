package com.finapse.service;

import com.finapse.dto.RawTransactionRecord;
import com.finapse.entity.Account;
import com.finapse.entity.Statement;
import com.finapse.entity.Transaction;
import com.finapse.enums.ReconciliationStatus;
import com.finapse.enums.TransactionDirection;
import com.finapse.enums.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class TransactionNormalizationServiceTest {

    private final TransactionNormalizationService service = new TransactionNormalizationService();

    @Test
    void normalize_setsAllFieldsCorrectly() {
        Account account = new Account();
        account.setId(UUID.randomUUID());

        Statement statement = new Statement();
        statement.setId(UUID.randomUUID());

        RawTransactionRecord raw = new RawTransactionRecord(
                5, LocalDate.of(2024, 8, 15), null,
                "  swiggy india pvt ltd  ",
                new BigDecimal("500.00"), TransactionDirection.DEBIT,
                "500.00", null, "15/08/2024"
        );

        Transaction tx = service.normalize(raw, statement, account, null);

        assertThat(tx.getDescription()).isEqualTo("SWIGGY INDIA PVT LTD");
        assertThat(tx.getAmount()).isEqualByComparingTo("500.00");
        assertThat(tx.getDirection()).isEqualTo(TransactionDirection.DEBIT);
        assertThat(tx.getTransactionType()).isEqualTo(TransactionType.UNKNOWN);
        assertThat(tx.getReconciliationStatus()).isEqualTo(ReconciliationStatus.UNMATCHED);
        assertThat(tx.getSourceRowNumber()).isEqualTo(5);
        assertThat(tx.getTransactionHash()).isNotNull().hasSize(64);
        assertThat(tx.getStatement()).isEqualTo(statement);
        assertThat(tx.getAccount()).isEqualTo(account);
        assertThat(tx.getCard()).isNull();
    }

    @Test
    void normalize_descriptionWhitespaceCollapsed() {
        Account account = new Account();
        account.setId(UUID.randomUUID());

        RawTransactionRecord raw = new RawTransactionRecord(
                1, LocalDate.now(), null,
                "  AMAZON   PURCHASE  ",
                BigDecimal.TEN, TransactionDirection.DEBIT,
                "10.00", null, "01/08/2024"
        );

        Transaction tx = service.normalize(raw, new Statement(), account, null);
        assertThat(tx.getDescription()).isEqualTo("AMAZON PURCHASE");
    }

    @Test
    void normalize_sameInputProducesSameHash() {
        Account account = new Account();
        account.setId(UUID.fromString("00000000-0000-0000-0000-000000000099"));

        RawTransactionRecord raw = new RawTransactionRecord(
                1, LocalDate.of(2024, 8, 1), null,
                "SALARY", new BigDecimal("26399.00"),
                TransactionDirection.CREDIT,
                null, "26399.00", "01/08/2024"
        );

        Transaction tx1 = service.normalize(raw, new Statement(), account, null);
        Transaction tx2 = service.normalize(raw, new Statement(), account, null);
        assertThat(tx1.getTransactionHash()).isEqualTo(tx2.getTransactionHash());
    }
}
