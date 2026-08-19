package com.finapse.classification.model;

import com.finapse.entity.Category;
import com.finapse.entity.Merchant;
import com.finapse.enums.ClassificationSource;
import com.finapse.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassificationResult {

    private TransactionType transactionType;
    private Category category;
    private Merchant merchant;
    private double confidence;
    private ClassificationSource source;
    private String reason;
    private boolean needsReview;

    public static ClassificationResultBuilder builder() {
        return new ClassificationResultBuilder();
    }
}
