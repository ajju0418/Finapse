package com.finapse.classification.detection;

import com.finapse.entity.Merchant;

public record MerchantMatchResult(Merchant merchant, double confidence, MatchType matchType) {

    public static MerchantMatchResult none() {
        return new MerchantMatchResult(null, 0, MatchType.NONE);
    }

    public boolean isPresent() {
        return merchant != null;
    }

    public enum MatchType {
        EXACT, CONTAINS, TOKEN_OVERLAP, NONE
    }
}
