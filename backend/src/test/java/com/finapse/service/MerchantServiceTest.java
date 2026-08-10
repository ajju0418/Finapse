package com.finapse.service;

import com.finapse.repository.MerchantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MerchantServiceTest {

    private MerchantService service;

    @BeforeEach
    void setUp() {
        service = new MerchantService(mock(MerchantRepository.class));
    }

    @Test
    void stripsLegalSuffix() {
        assertThat(service.normalizeMerchantName("SWIGGY INDIA PVT LTD")).isEqualTo("SWIGGY");
    }

    @Test
    void stripsUpiPrefix() {
        assertThat(service.normalizeMerchantName("UPI-ZOMATO-REF123456")).isEqualTo("ZOMATO-REF123456");
    }

    @Test
    void splitsOnAsterisk() {
        assertThat(service.normalizeMerchantName("AMAZON PAY*ORDER 123")).isEqualTo("AMAZON PAY");
    }

    @Test
    void truncatesLongDescription() {
        assertThat(service.normalizeMerchantName("SOME VERY LONG MERCHANT NAME WITH EXTRA WORDS"))
                .isEqualTo("SOME VERY");
    }

    @Test
    void handlesBlankInput() {
        assertThat(service.normalizeMerchantName("")).isEqualTo("");
        assertThat(service.normalizeMerchantName(null)).isEqualTo("");
    }

    @Test
    void collapsesDuplicateSpaces() {
        assertThat(service.normalizeMerchantName("NETFLIX  INDIA")).isEqualTo("NETFLIX");
    }
}
