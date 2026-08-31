package com.finapse.service;

import com.finapse.entity.Transaction;
import com.finapse.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Exports the signed-in user's data so it is never locked inside the product.
 *
 * <p>Values are escaped for RFC 4180 and any leading {@code = + - @} is prefixed
 * with a single quote, so a crafted merchant name cannot become a live formula
 * when the file is opened in a spreadsheet.
 */
@Service
@RequiredArgsConstructor
public class ExportService {

    private static final String[] HEADERS = {
            "Date", "Posted Date", "Description", "Merchant", "Category",
            "Amount", "Direction", "Type", "Cashback", "Reconciliation Status",
            "Classification Source", "Confidence", "Reason", "Recurring", "Statement"
    };

    private final TransactionRepository transactionRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public String exportTransactionsCsv(LocalDate from, LocalDate to) {
        UUID userId = userService.getCurrentUserId();
        LocalDate start = from != null ? from : LocalDate.of(1970, 1, 1);
        LocalDate end = to != null ? to : LocalDate.now();

        List<Transaction> transactions = transactionRepository.findByUserAndDateRange(userId, start, end);

        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", HEADERS)).append("\r\n");

        transactions.stream()
                .sorted((a, b) -> b.getTransactionDate().compareTo(a.getTransactionDate()))
                .forEach(tx -> csv.append(row(tx)).append("\r\n"));

        return csv.toString();
    }

    private String row(Transaction tx) {
        return String.join(",",
                cell(tx.getTransactionDate()),
                cell(tx.getPostedDate()),
                cell(tx.getDescription()),
                cell(tx.getMerchant() != null ? tx.getMerchant().getName() : null),
                cell(tx.getCategory() != null ? tx.getCategory().getName() : null),
                cell(tx.getAmount()),
                cell(tx.getDirection()),
                cell(tx.getTransactionType()),
                cell(tx.getCashbackAmount()),
                cell(tx.getReconciliationStatus()),
                cell(tx.getClassificationSource()),
                cell(tx.getClassificationConfidence()),
                cell(tx.getClassificationReason()),
                cell(Boolean.TRUE.equals(tx.getIsRecurring()) ? "Yes" : "No"),
                cell(tx.getStatement() != null ? tx.getStatement().getOriginalFileName() : null));
    }

    private String cell(Object value) {
        if (value == null) return "";
        String text = value.toString();

        // Neutralise spreadsheet formula injection before quoting.
        if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        return '"' + text.replace("\"", "\"\"") + '"';
    }
}
