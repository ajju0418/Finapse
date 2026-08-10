package com.finapse.service;

import com.finapse.dto.RawTransactionRecord;
import com.finapse.entity.Account;
import com.finapse.entity.Card;
import com.finapse.entity.Statement;
import com.finapse.entity.Transaction;
import com.finapse.enums.ReconciliationStatus;
import com.finapse.enums.TransactionType;
import com.finapse.util.HashUtil;
import org.springframework.stereotype.Service;

/**
 * Converts a RawTransactionRecord into a Transaction entity.
 * Normalises descriptions, generates the transaction fingerprint hash,
 * and preserves full source traceability.
 * Does NOT classify transaction type — that belongs to TransactionClassificationService.
 */
@Service
public class TransactionNormalizationService {

    public Transaction normalize(RawTransactionRecord raw, Statement statement,
                                 Account account, Card card) {
        Transaction tx = new Transaction();
        tx.setStatement(statement);
        tx.setAccount(account);
        tx.setCard(card);

        tx.setTransactionDate(raw.transactionDate());
        tx.setPostedDate(raw.postedDate());
        tx.setDescription(normalizeDescription(raw.description()));
        tx.setAmount(raw.amount());
        tx.setDirection(raw.direction());
        tx.setTransactionType(TransactionType.UNKNOWN);
        tx.setReconciliationStatus(ReconciliationStatus.UNMATCHED);
        tx.setSourceRowNumber(raw.sourceRowNumber());

        tx.setTransactionHash(buildHash(raw, account, card));

        return tx;
    }

    private String normalizeDescription(String raw) {
        if (raw == null) return "";
        // Collapse whitespace, trim, uppercase for consistent matching
        return raw.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    /**
     * Fingerprint: source-id + date + amount + direction + normalised description.
     * Used as a duplicate detection aid — not proof of duplication.
     */
    private String buildHash(RawTransactionRecord raw, Account account, Card card) {
        String sourceId = account != null ? account.getId().toString() : card.getId().toString();
        String input = sourceId
                + "|" + raw.transactionDate()
                + "|" + raw.amount().toPlainString()
                + "|" + raw.direction().name()
                + "|" + normalizeDescription(raw.description());
        return HashUtil.sha256(input);
    }
}
