package com.finapse.classification.detection;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class NormalizationService {

    private static final List<String> INDIAN_CITIES = List.of(
            "MUMBAI", "DELHI", "BANGALORE", "BENGALURU", "CHENNAI", "HYDERABAD",
            "KOLKATA", "PUNE", "AHMEDABAD", "JAIPUR", "LUCKNOW", "SURAT",
            "CHANDIGARH", "NOIDA", "GURGAON", "GURUGRAM", "GHAZIABAD",
            "FARIDABAD", "THANE", "NAVI MUMBAI", "INDORE", "BHOPAL",
            "NAGPUR", "VISAKHAPATNAM", "COIMBATORE", "KOCHI", "MYSORE",
            "MYSURU", "MANGALORE", "MANGALURU", "TRIVANDRUM", "THIRUVANANTHAPURAM",
            "VADODARA", "PATNA", "RANCHI", "DEHRADUN", "AGRA", "VARANASI",
            "MADURAI", "RAJKOT", "NASHIK", "AURANGABAD", "VIJAYAWADA",
            "HUBLI", "DHARWAD", "TIRUCHIRAPPALLI", "SALEM", "WARANGAL"
    );

    private static final Pattern UPI_PREFIX = Pattern.compile("^UPI[-/]");
    private static final Pattern NEFT_PREFIX = Pattern.compile("^NEFT[-/ ](?:CR|DR)?[-/ ]?");
    private static final Pattern IMPS_PREFIX = Pattern.compile("^IMPS[-/](?:P2A|P2P)?[-/]?");
    private static final Pattern RTGS_PREFIX = Pattern.compile("^RTGS[-/]");
    private static final Pattern NACH_PREFIX = Pattern.compile("^(?:NACH|ACH|ECS)[-/ ]?(?:DR|CR)?[-/ ]?");

    private static final Pattern VPA_HANDLE = Pattern.compile("@[A-Z0-9]+");
    private static final Pattern PHONE_NUMBER = Pattern.compile("\\b\\d{10}\\b");
    private static final Pattern TRANSACTION_REF = Pattern.compile("\\b(?:TXN|REF|UTR|RRN|ARN|AUTH)[-:]?\\s*[A-Z0-9]{6,}\\b");
    private static final Pattern POS_TERMINAL = Pattern.compile("\\bPOS\\s*(?:TXN|TERMINAL|T/N)?\\s*\\d+\\b");
    private static final Pattern MCC_CODE = Pattern.compile("\\bMCC[-:]?\\s*\\d{4}\\b");
    private static final Pattern LONG_NUMERIC_ID = Pattern.compile("\\b\\d{8,}\\b");
    private static final Pattern DATE_PATTERN = Pattern.compile("\\b\\d{2}[-/]\\d{2}[-/](?:\\d{2}|\\d{4})\\b");
    private static final Pattern TRAILING_NUMERIC = Pattern.compile("[-/]\\d+$");

    private static final Pattern GPAY = Pattern.compile("-?GPAY-?");
    private static final Pattern PAYTM = Pattern.compile("-?PAYTM-?");
    private static final Pattern PHONEPE = Pattern.compile("-?PHONEPE-?");
    private static final Pattern PAID_VIA = Pattern.compile("-?PAID VIA.*");
    private static final Pattern GATEWAY_SUFFIX = Pattern.compile("\\b(?:RAZORPAY|CASHFREE|PAYU|CCAVENUE|BILLDESK)\\b");

    public String normalize(String rawNarration) {
        if (rawNarration == null) return "";

        String normalized = rawNarration.toUpperCase().trim();

        normalized = removePaymentPrefixes(normalized);
        normalized = removePaymentGateways(normalized);
        normalized = removeIdentifiers(normalized);
        normalized = removeCityNames(normalized);
        normalized = cleanupFormatting(normalized);

        return normalized;
    }

    public String extractMccCode(String rawNarration) {
        if (rawNarration == null) return null;
        var matcher = MCC_CODE.matcher(rawNarration.toUpperCase());
        if (matcher.find()) {
            return matcher.group().replaceAll("[^0-9]", "");
        }
        return null;
    }

    private String removePaymentPrefixes(String text) {
        text = UPI_PREFIX.matcher(text).replaceAll("");
        text = NEFT_PREFIX.matcher(text).replaceAll("");
        text = IMPS_PREFIX.matcher(text).replaceAll("");
        text = RTGS_PREFIX.matcher(text).replaceAll("");
        text = NACH_PREFIX.matcher(text).replaceAll("");
        return text;
    }

    private String removePaymentGateways(String text) {
        text = GPAY.matcher(text).replaceAll(" ");
        text = PAYTM.matcher(text).replaceAll(" ");
        text = PHONEPE.matcher(text).replaceAll(" ");
        text = PAID_VIA.matcher(text).replaceAll("");
        text = GATEWAY_SUFFIX.matcher(text).replaceAll("");
        return text;
    }

    private String removeIdentifiers(String text) {
        text = VPA_HANDLE.matcher(text).replaceAll("");
        text = PHONE_NUMBER.matcher(text).replaceAll("");
        text = TRANSACTION_REF.matcher(text).replaceAll("");
        text = POS_TERMINAL.matcher(text).replaceAll("");
        text = MCC_CODE.matcher(text).replaceAll("");
        text = LONG_NUMERIC_ID.matcher(text).replaceAll("");
        text = DATE_PATTERN.matcher(text).replaceAll("");

        text = text.replaceAll("[-/]+$", "").replaceAll("^[-/]+", "");

        while (TRAILING_NUMERIC.matcher(text).find()) {
            text = TRAILING_NUMERIC.matcher(text).replaceAll("");
        }

        return text;
    }

    private String removeCityNames(String text) {
        for (String city : INDIAN_CITIES) {
            String withSpace = " " + city + " ";
            String atEnd = " " + city;
            if (text.endsWith(atEnd)) {
                text = text.substring(0, text.length() - atEnd.length());
            } else {
                text = text.replace(withSpace, " ");
            }
        }
        text = text.replaceAll("\\b[A-Z]{2}\\s*$", "");
        return text;
    }

    private String cleanupFormatting(String text) {
        text = text.replaceAll("[-/]+$", "");
        text = text.replaceAll("^[-/]+", "");
        text = text.replaceAll("\\s+", " ").trim();
        return text;
    }
}
