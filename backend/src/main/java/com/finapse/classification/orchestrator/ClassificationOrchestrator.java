package com.finapse.classification.orchestrator;

import com.finapse.classification.detection.MerchantDetectionService;
import com.finapse.classification.detection.NormalizationService;
import com.finapse.classification.model.ClassificationContext;
import com.finapse.classification.model.ClassificationResult;
import com.finapse.classification.strategy.ClassificationStrategy;
import com.finapse.dto.RawTransactionRecord;
import com.finapse.entity.Account;
import com.finapse.entity.Card;
import com.finapse.entity.Merchant;
import com.finapse.entity.Statement;
import com.finapse.entity.Transaction;
import com.finapse.enums.ClassificationSource;
import com.finapse.enums.ReconciliationStatus;
import com.finapse.enums.TransactionType;
import com.finapse.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassificationOrchestrator {

    private final NormalizationService normalizationService;
    private final MerchantDetectionService merchantDetectionService;
    private final List<ClassificationStrategy> strategies;

    public Transaction orchestrate(RawTransactionRecord raw, Statement statement, Account account, Card card) {
        // 1. Build initial Transaction entity
        Transaction tx = new Transaction();
        tx.setStatement(statement);
        tx.setAccount(account);
        tx.setCard(card);
        tx.setTransactionDate(raw.transactionDate());
        tx.setPostedDate(raw.postedDate());
        tx.setDescription(raw.description()); // Keep raw description
        tx.setAmount(raw.amount());
        tx.setDirection(raw.direction());
        tx.setReconciliationStatus(ReconciliationStatus.UNMATCHED);
        tx.setSourceRowNumber(raw.sourceRowNumber());

        // 2. Normalize and extract Merchant
        String normalizedNarration = normalizationService.normalize(raw.description());
        Optional<Merchant> merchantOpt = merchantDetectionService.detect(normalizedNarration);
        
        tx.setTransactionHash(buildHash(raw, account, card, normalizedNarration));
        merchantOpt.ifPresent(tx::setMerchant);

        return processClassification(tx, raw.description(), normalizedNarration, merchantOpt);
    }

    public Transaction reclassify(Transaction tx) {
        String normalizedNarration = normalizationService.normalize(tx.getDescription());
        Optional<Merchant> merchantOpt = merchantDetectionService.detect(normalizedNarration);
        merchantOpt.ifPresent(tx::setMerchant);
        return processClassification(tx, tx.getDescription(), normalizedNarration, merchantOpt);
    }

    private Transaction processClassification(Transaction tx, String rawDesc, String normalizedDesc, Optional<Merchant> merchantOpt) {
        // 3. Build Context
        ClassificationContext context = ClassificationContext.builder()
                .transaction(tx)
                .rawNarration(rawDesc)
                .normalizedNarration(normalizedDesc)
                .amount(tx.getAmount())
                .direction(tx.getDirection())
                .merchant(merchantOpt.orElse(null))
                .build();

        // 4. Run through Strategies based on Priority
        ClassificationResult finalResult = null;

        for (ClassificationStrategy strategy : strategies) {
            if (strategy.supports(context)) {
                ClassificationResult result = strategy.classify(context);
                if (result != null) {
                    if (finalResult == null || result.getConfidence() > finalResult.getConfidence()) {
                        finalResult = result;
                    }
                    
                    // Stop if we have a very high confidence match
                    if (finalResult.getConfidence() >= 0.90) {
                        break;
                    }
                }
            }
        }

        // 5. Apply Results to Transaction
        if (finalResult != null) {
            tx.setTransactionType(finalResult.getTransactionType() != null ? finalResult.getTransactionType() : TransactionType.UNKNOWN);
            
            // The merchant and category might have been enriched by the classifier (e.g. LLM or Rule based)
            if (finalResult.getMerchant() != null) {
                tx.setMerchant(finalResult.getMerchant());
            }
            if (finalResult.getCategory() != null) {
                tx.setCategory(finalResult.getCategory());
            }
            
            if (finalResult.isNeedsReview()) {
                log.info("Transaction needs review: {}", tx.getDescription());
                // In future, set a specific review flag if added to DB
            }
        } else {
            tx.setTransactionType(TransactionType.UNKNOWN);
        }

        return tx;
    }

    private String buildHash(RawTransactionRecord raw, Account account, Card card, String normalizedDesc) {
        String sourceId = account != null ? account.getId().toString() : card.getId().toString();
        String input = sourceId
                + "|" + raw.transactionDate()
                + "|" + raw.amount().toPlainString()
                + "|" + raw.direction().name()
                + "|" + normalizedDesc;
        return HashUtil.sha256(input);
    }
}
