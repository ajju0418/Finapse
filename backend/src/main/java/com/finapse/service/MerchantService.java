package com.finapse.service;

import com.finapse.entity.Merchant;
import com.finapse.entity.Transaction;
import com.finapse.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Resolves a Merchant from a transaction description.
 * Normalises the raw description to a canonical merchant name and
 * finds-or-creates the Merchant record.
 */
@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;

    public Merchant resolveForTransaction(Transaction tx) {
        String normalized = normalizeMerchantName(tx.getDescription());
        if (normalized.isBlank()) return null;

        return merchantRepository.findByNormalizedName(normalized)
                .orElseGet(() -> {
                    Merchant m = new Merchant();
                    m.setName(normalized);
                    m.setNormalizedName(normalized);
                    return merchantRepository.save(m);
                });
    }

    /**
     * Strips common bank suffixes and noise from a normalised description.
     *
     * Examples:
     *   "SWIGGY INDIA PVT LTD"  → "SWIGGY"
     *   "AMAZON PAY*ORDER 123"  → "AMAZON PAY"
     *   "UPI-ZOMATO-REF123"     → "ZOMATO"
     */
    String normalizeMerchantName(String description) {
        if (description == null || description.isBlank()) return "";

        String name = description.trim().toUpperCase();

        // Strip UPI prefix: "UPI-MERCHANT-REF" → "MERCHANT-REF"
        name = name.replaceAll("^UPI[-/]", "");

        // Strip payment gateway prefixes before splitting on "*":
        // "RAZ*SWIGGY" → "SWIGGY", "CASHFREE*FLIPKART" → "FLIPKART", "PAY*VODAFONEIDEA" → "VODAFONEIDEA"
        name = name.replaceAll("^(RAZ|CASHFREE|PAYU|PHONEPE|GPAY|PAY)\\*", "");

        // Split on remaining delimiters — preserves "AMAZON PAY" from "AMAZON PAY*ORDER 123"
        name = name.split("[*|@#/]")[0].trim();

        // Remove legal suffixes (PAY intentionally excluded)
        name = name.replaceAll("\\b(PVT\\.?\\s*LTD\\.?|LTD\\.?|INC\\.?|CORP\\.?|LLC\\.?|INDIA|TECHNOLOGIES|TECH|SERVICES|PAYMENTS)\\b", "").trim();

        // Collapse multiple spaces
        name = name.replaceAll("\\s{2,}", " ").trim();

        // Truncate very long descriptions to first 2 words
        String[] words = name.split("\\s+");
        if (words.length > 4) {
            name = words[0] + (words.length > 1 ? " " + words[1] : "");
        }

        return name.trim();
    }
}
