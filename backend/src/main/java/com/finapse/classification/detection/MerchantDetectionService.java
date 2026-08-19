package com.finapse.classification.detection;

import com.finapse.entity.Merchant;
import com.finapse.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MerchantDetectionService {

    private final MerchantRepository merchantRepository;

    public Optional<Merchant> detect(String normalizedNarration) {
        if (normalizedNarration == null || normalizedNarration.isEmpty()) {
            return Optional.empty();
        }
        
        // Exact match
        Optional<Merchant> exactMatch = merchantRepository.findByNormalizedName(normalizedNarration);
        if (exactMatch.isPresent()) {
            return exactMatch;
        }

        // We could add fuzzy matching here later or loop through all merchants
        // For production scale, an inverted index or Elasticsearch would be better.
        // For now, if the normalized narration contains the merchant normalized name (for larger string)
        // or just return empty if no exact match.
        
        return Optional.empty();
    }
}
