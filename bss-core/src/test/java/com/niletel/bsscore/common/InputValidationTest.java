package com.niletel.bsscore.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputValidationTest {

    @Test
    void validatesMsisdnFormat() {
        assertTrue(InputValidation.isMsisdn("01012345678"));
        assertTrue(InputValidation.isMsisdn("01112345678"));
        assertTrue(InputValidation.isMsisdn("01212345678"));
        assertTrue(InputValidation.isMsisdn("01512345678"));

        assertFalse(InputValidation.isMsisdn(null));
        assertFalse(InputValidation.isMsisdn(""));
        assertFalse(InputValidation.isMsisdn("0101234567"));
        assertFalse(InputValidation.isMsisdn("01312345678"));
        assertFalse(InputValidation.isMsisdn(" 01012345678"));
        assertFalse(InputValidation.isMsisdn("01012345678 "));
    }

    @Test
    void validatesPinFormatWithoutTrimming() {
        assertTrue(InputValidation.isPin("1234"));

        assertFalse(InputValidation.isPin(null));
        assertFalse(InputValidation.isPin(""));
        assertFalse(InputValidation.isPin("123"));
        assertFalse(InputValidation.isPin("12345"));
        assertFalse(InputValidation.isPin("12a4"));
        assertFalse(InputValidation.isPin(" 1234"));
        assertFalse(InputValidation.isPin("1234 "));
    }
}
