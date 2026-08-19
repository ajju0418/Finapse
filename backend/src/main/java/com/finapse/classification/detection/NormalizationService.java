package com.finapse.classification.detection;

import org.springframework.stereotype.Service;

@Service
public class NormalizationService {

    public String normalize(String rawNarration) {
        if (rawNarration == null) return "";
        
        String normalized = rawNarration.toUpperCase().trim();
        
        // Remove common UPI prefixes
        normalized = normalized.replaceAll("^UPI[-/]", "");
        normalized = normalized.replaceAll("^NEFT[-/ ](?:CR|DR)?[-/ ]?", "");
        normalized = normalized.replaceAll("^IMPS[-/](?:P2A|P2P)?[-/]", "");
        normalized = normalized.replaceAll("^RTGS[-/]", "");
        
        // Remove trailing VPA handles (e.g. @okhdfcbank, @ybl)
        normalized = normalized.replaceAll("@[A-Z]+", "");
        
        // Remove phone numbers commonly found in UPI strings
        normalized = normalized.replaceAll("\\b\\d{10}\\b", "");
        
        // Remove common gateway artifacts
        normalized = normalized.replaceAll("-GPAY-", "-");
        normalized = normalized.replaceAll("-PAYTM-", "-");
        normalized = normalized.replaceAll("-PHONEPE-", "-");
        normalized = normalized.replaceAll("-PAID VIA.*", "");
        
        // Cleanup trailing/leading special characters and extra spaces
        normalized = normalized.replaceAll("[-/]+$", "");
        normalized = normalized.replaceAll("^[-/]+", "");
        
        // Remove trailing numeric identifiers (reference numbers, long IDs) repeatedly
        while (normalized.matches(".*[-/]\\d+$")) {
            normalized = normalized.replaceAll("[-/]\\d+$", "");
        }
        
        normalized = normalized.replaceAll("\\s+", " ").trim();
        
        return normalized;
    }
}
