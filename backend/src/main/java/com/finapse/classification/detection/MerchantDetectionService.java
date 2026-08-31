package com.finapse.classification.detection;

import com.finapse.entity.Merchant;
import com.finapse.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MerchantDetectionService {

    private final MerchantRepository merchantRepository;

    public Optional<Merchant> detect(String normalizedNarration) {
        if (normalizedNarration == null || normalizedNarration.isEmpty()) {
            return Optional.empty();
        }

        Optional<Merchant> exactMatch = merchantRepository.findByNormalizedName(normalizedNarration);
        if (exactMatch.isPresent()) {
            return exactMatch;
        }

        Optional<Merchant> containsMatch = findByContainsMatch(normalizedNarration);
        if (containsMatch.isPresent()) {
            return containsMatch;
        }

        return findByTokenOverlap(normalizedNarration);
    }

    public MerchantMatchResult detectWithConfidence(String normalizedNarration) {
        if (normalizedNarration == null || normalizedNarration.isEmpty()) {
            return MerchantMatchResult.none();
        }

        Optional<Merchant> exactMatch = merchantRepository.findByNormalizedName(normalizedNarration);
        if (exactMatch.isPresent()) {
            return new MerchantMatchResult(exactMatch.get(), 1.0, MerchantMatchResult.MatchType.EXACT);
        }

        Optional<Merchant> containsMatch = findByContainsMatch(normalizedNarration);
        if (containsMatch.isPresent()) {
            double confidence = computeContainsConfidence(containsMatch.get().getNormalizedName(), normalizedNarration);
            return new MerchantMatchResult(containsMatch.get(), confidence, MerchantMatchResult.MatchType.CONTAINS);
        }

        var tokenResult = findByTokenOverlapWithScore(normalizedNarration);
        if (tokenResult != null) {
            return tokenResult;
        }

        return MerchantMatchResult.none();
    }

    private Optional<Merchant> findByContainsMatch(String normalizedNarration) {
        List<Merchant> matches = merchantRepository.findByNarrationContaining(normalizedNarration);
        if (!matches.isEmpty()) {
            return Optional.of(matches.get(0));
        }
        return Optional.empty();
    }

    private Optional<Merchant> findByTokenOverlap(String normalizedNarration) {
        var result = findByTokenOverlapWithScore(normalizedNarration);
        if (result != null && result.merchant() != null) {
            return Optional.of(result.merchant());
        }
        return Optional.empty();
    }

    private MerchantMatchResult findByTokenOverlapWithScore(String normalizedNarration) {
        String[] narrationTokens = normalizedNarration.split("\\s+");
        if (narrationTokens.length == 0) return null;

        String firstToken = narrationTokens[0];
        if (firstToken.length() < 3) return null;

        List<Merchant> candidates = merchantRepository.findByNormalizedNameContaining(firstToken);
        if (candidates.isEmpty()) return null;

        Merchant bestMatch = null;
        double bestScore = 0;

        for (Merchant candidate : candidates) {
            double score = computeTokenOverlap(narrationTokens, candidate.getNormalizedName().split("\\s+"));
            if (score > bestScore) {
                bestScore = score;
                bestMatch = candidate;
            }
        }

        if (bestMatch != null && bestScore >= 0.5) {
            double confidence = 0.60 + (bestScore * 0.30);
            return new MerchantMatchResult(bestMatch, confidence, MerchantMatchResult.MatchType.TOKEN_OVERLAP);
        }
        return null;
    }

    private double computeTokenOverlap(String[] narrationTokens, String[] merchantTokens) {
        if (merchantTokens.length == 0) return 0;
        int matches = 0;
        for (String mt : merchantTokens) {
            for (String nt : narrationTokens) {
                if (nt.equals(mt) || levenshteinDistance(nt, mt) <= 2) {
                    matches++;
                    break;
                }
            }
        }
        return (double) matches / merchantTokens.length;
    }

    private double computeContainsConfidence(String merchantName, String narration) {
        double ratio = (double) merchantName.length() / narration.length();
        return Math.min(0.95, 0.80 + (ratio * 0.15));
    }

    private int levenshteinDistance(String a, String b) {
        int lenA = a.length();
        int lenB = b.length();
        if (Math.abs(lenA - lenB) > 3) return Math.abs(lenA - lenB);

        int[] prev = new int[lenB + 1];
        int[] curr = new int[lenB + 1];

        for (int j = 0; j <= lenB; j++) prev[j] = j;

        for (int i = 1; i <= lenA; i++) {
            curr[0] = i;
            for (int j = 1; j <= lenB; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }
        return prev[lenB];
    }
}
