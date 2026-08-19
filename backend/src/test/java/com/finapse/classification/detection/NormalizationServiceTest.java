package com.finapse.classification.detection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NormalizationServiceTest {

    private NormalizationService normalizationService;

    @BeforeEach
    void setUp() {
        normalizationService = new NormalizationService();
    }

    @Test
    void testNormalizeUpi() {
        assertEquals("ZOMATO", normalizationService.normalize("UPI-ZOMATO-GPAY-12203264768"));
        assertEquals("SWIGGY", normalizationService.normalize("UPI-SWIGGY-PAYTM-12203264768@okhdfcbank"));
        assertEquals("ZUBAITHA RESTAURANT", normalizationService.normalize("UPI/ZUBAITHA RESTAURANT/12345/1234567890@ybl"));
    }

    @Test
    void testNormalizeNeft() {
        assertEquals("COGNIZANT SAL", normalizationService.normalize("NEFT CR-COGNIZANT SAL"));
        assertEquals("SOME CORP", normalizationService.normalize("NEFT-SOME CORP"));
    }

    @Test
    void testNormalizeImpsRtgs() {
        assertEquals("JOHN DOE", normalizationService.normalize("IMPS/P2A/JOHN DOE/123456"));
        assertEquals("JANE DOE", normalizationService.normalize("RTGS-JANE DOE"));
    }

    @Test
    void testPaidVia() {
        assertEquals("AMAZON", normalizationService.normalize("AMAZON-PAID VIA SUPERMONEY"));
    }
}
