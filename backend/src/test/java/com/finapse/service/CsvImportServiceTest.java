package com.finapse.service;

import com.finapse.dto.StatementParseResult;
import com.finapse.enums.TransactionDirection;
import com.finapse.exception.InvalidStatementFileException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class CsvImportServiceTest {

    private final CsvImportService service = new CsvImportService();

    private ByteArrayInputStream csv(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void parse_debitCreditColumns_extractsCorrectly() {
        String content = """
                Date,Description,Debit,Credit
                15/08/2024,SWIGGY ORDER,500.00,
                01/08/2024,SALARY CREDIT,,26399.00
                """;
        StatementParseResult result = service.parse(csv(content), "test.csv");

        assertThat(result.records()).hasSize(2);
        assertThat(result.invalidRows()).isEmpty();

        var swiggy = result.records().get(0);
        assertThat(swiggy.description()).isEqualTo("SWIGGY ORDER");
        assertThat(swiggy.amount()).isEqualByComparingTo("500.00");
        assertThat(swiggy.direction()).isEqualTo(TransactionDirection.DEBIT);
        assertThat(swiggy.transactionDate()).isEqualTo(LocalDate.of(2024, 8, 15));
        assertThat(swiggy.sourceRowNumber()).isEqualTo(2);

        var salary = result.records().get(1);
        assertThat(salary.direction()).isEqualTo(TransactionDirection.CREDIT);
        assertThat(salary.amount()).isEqualByComparingTo("26399.00");
    }

    @Test
    void parse_singleAmountColumn_negativeIsDebit() {
        String content = """
                Transaction Date,Narration,Amount
                10/08/2024,AMAZON PURCHASE,-1500.00
                01/08/2024,CASHBACK,75.00
                """;
        StatementParseResult result = service.parse(csv(content), "test.csv");

        assertThat(result.records()).hasSize(2);
        assertThat(result.records().get(0).direction()).isEqualTo(TransactionDirection.DEBIT);
        assertThat(result.records().get(0).amount()).isEqualByComparingTo("1500.00");
        assertThat(result.records().get(1).direction()).isEqualTo(TransactionDirection.CREDIT);
    }

    @Test
    void parse_multipleDataFormats_allParsed() {
        String content = """
                Date,Description,Debit,Credit
                15-08-2024,UPI PAYMENT,200.00,
                2024-08-16,NEFT CREDIT,,5000.00
                15 Aug 2024,ATM WITHDRAWAL,1000.00,
                """;
        StatementParseResult result = service.parse(csv(content), "test.csv");
        assertThat(result.records()).hasSize(3);
        assertThat(result.invalidRows()).isEmpty();
    }

    @Test
    void parse_invalidRow_reportedNotSilentlyDropped() {
        String content = """
                Date,Description,Debit,Credit
                15/08/2024,VALID ROW,500.00,
                BADDATE,INVALID DATE ROW,100.00,
                """;
        StatementParseResult result = service.parse(csv(content), "test.csv");

        assertThat(result.records()).hasSize(1);
        assertThat(result.invalidRows()).hasSize(1);
        assertThat(result.invalidRows().get(0).rowNumber()).isEqualTo(3);
    }

    @Test
    void parse_amountWithCurrencySymbolAndCommas_parsedCorrectly() {
        // Real bank CSVs quote cells containing commas
        String content = """
                Date,Description,Debit,Credit
                01/08/2024,SBI CARD PAYMENT,"₹1,00,000.00",
                """;
        StatementParseResult result = service.parse(csv(content), "test.csv");
        assertThat(result.records()).hasSize(1);
        assertThat(result.records().get(0).amount()).isEqualByComparingTo("100000.00");
    }

    @Test
    void parse_missingDateColumn_throwsInvalidStatementFileException() {
        String content = """
                Description,Debit,Credit
                SWIGGY,500.00,
                """;
        assertThatThrownBy(() -> service.parse(csv(content), "test.csv"))
                .isInstanceOf(InvalidStatementFileException.class)
                .hasMessageContaining("date column");
    }

    @Test
    void parse_emptyFile_throwsInvalidStatementFileException() {
        assertThatThrownBy(() -> service.parse(csv(""), "test.csv"))
                .isInstanceOf(InvalidStatementFileException.class);
    }

    @Test
    void parse_narrationAlias_resolvedToDescription() {
        String content = """
                Date,Narration,Withdrawal,Deposit
                10/08/2024,SWIGGY FOOD,350.00,
                """;
        StatementParseResult result = service.parse(csv(content), "test.csv");
        assertThat(result.records()).hasSize(1);
        assertThat(result.records().get(0).description()).isEqualTo("SWIGGY FOOD");
        assertThat(result.records().get(0).direction()).isEqualTo(TransactionDirection.DEBIT);
    }
}
