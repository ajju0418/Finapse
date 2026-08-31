package com.finapse.service;

import com.finapse.entity.Merchant;
import com.finapse.entity.Statement;
import com.finapse.entity.Transaction;
import com.finapse.enums.TransactionDirection;
import com.finapse.enums.TransactionType;
import com.finapse.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock UserService userService;
    @InjectMocks ExportService exportService;

    @Test
    void export_writesAHeaderRowAndOneRowPerTransaction() {
        when(userService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(transactionRepository.findByUserAndDateRange(any(), any(), any()))
                .thenReturn(List.of(tx("SWIGGY ORDER", "450.00")));

        String csv = exportService.exportTransactionsCsv(null, null);
        String[] lines = csv.split("\r\n");

        assertThat(lines).hasSize(2);
        assertThat(lines[0]).startsWith("Date,Posted Date,Description");
        assertThat(lines[1]).contains("\"SWIGGY ORDER\"").contains("\"450.00\"");
    }

    @Test
    void export_escapesEmbeddedQuotesAndCommas() {
        when(userService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(transactionRepository.findByUserAndDateRange(any(), any(), any()))
                .thenReturn(List.of(tx("BIG \"BAZAAR\", MUMBAI", "100.00")));

        String csv = exportService.exportTransactionsCsv(null, null);

        // Quotes are doubled and the whole cell is quoted, so the comma stays inside it.
        assertThat(csv).contains("\"BIG \"\"BAZAAR\"\", MUMBAI\"");
    }

    @Test
    void export_neutralisesSpreadsheetFormulaInjection() {
        when(userService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(transactionRepository.findByUserAndDateRange(any(), any(), any()))
                .thenReturn(List.of(tx("=cmd|'/c calc'!A1", "1.00")));

        String csv = exportService.exportTransactionsCsv(null, null);

        // A leading '=' would execute when opened in Excel; it must be quoted out.
        assertThat(csv).contains("\"'=cmd|'/c calc'!A1\"");
        assertThat(csv).doesNotContain(",\"=cmd");
    }

    @Test
    void export_emitsOnlyTheHeaderWhenThereAreNoTransactions() {
        when(userService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(transactionRepository.findByUserAndDateRange(any(), any(), any()))
                .thenReturn(List.of());

        assertThat(exportService.exportTransactionsCsv(null, null).split("\r\n")).hasSize(1);
    }

    private Transaction tx(String description, String amount) {
        Statement statement = new Statement();
        statement.setOriginalFileName("hdfc.csv");

        Merchant merchant = new Merchant();
        merchant.setName("SWIGGY");

        Transaction tx = new Transaction();
        tx.setId(UUID.randomUUID());
        tx.setStatement(statement);
        tx.setMerchant(merchant);
        tx.setDescription(description);
        tx.setAmount(new BigDecimal(amount));
        tx.setDirection(TransactionDirection.DEBIT);
        tx.setTransactionType(TransactionType.EXPENSE);
        tx.setTransactionDate(LocalDate.of(2026, 3, 14));
        return tx;
    }
}
