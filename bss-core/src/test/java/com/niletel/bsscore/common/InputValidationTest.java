package com.niletel.bsscore.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @ParameterizedTest
    @ValueSource(strings = {"1", "100", "3500", "9999999", "10000000"})
    void parsesWholeNumbersInTheAllowedRange(String text) {
        assertEquals(OptionalInt.of(Integer.parseInt(text)), InputValidation.parseDebitAmount(text));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "00", "-5", "10000001", "99999999", "2147483648", "99999999999999999999"})
    void rejectsNumbersOutsideTheAllowedRange(String text) {
        assertEquals(OptionalInt.empty(), InputValidation.parseDebitAmount(text));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "abc", "12.5", "1e3", "+100", " 100", "100 ", "1 000", "1,000", "100\n",
            "\u0661\u0660\u0660"})
    void rejectsTextThatIsNotPlainAsciiDigits(String text) {
        assertEquals(OptionalInt.empty(), InputValidation.parseDebitAmount(text));
    }

    @Test
    void leadingZerosAreStillAWholeNumber() {
        assertEquals(OptionalInt.of(100), InputValidation.parseDebitAmount("0100"));
        assertEquals(OptionalInt.of(1), InputValidation.parseDebitAmount("00000000000001"));
        assertEquals(OptionalInt.of(10000000), InputValidation.parseDebitAmount("010000000"));
    }

    @Test
    void debitAmountRangeIsOneToTenMillion() {
        assertFalse(InputValidation.isDebitAmount(0));
        assertTrue(InputValidation.isDebitAmount(1));
        assertTrue(InputValidation.isDebitAmount(10000000));
        assertFalse(InputValidation.isDebitAmount(10000001));
    }
}
