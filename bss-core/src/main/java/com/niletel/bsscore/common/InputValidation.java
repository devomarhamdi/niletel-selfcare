package com.niletel.bsscore.common;

import java.util.OptionalInt;
import java.util.regex.Pattern;

public final class InputValidation {

    private static final Pattern MSISDN_PATTERN = Pattern.compile("^01[0125][0-9]{8}$");
    private static final Pattern PIN_PATTERN = Pattern.compile("^[0-9]{4}$");
    private static final Pattern TXN_REFNO_PATTERN = Pattern.compile("^[A-Z0-9-]{6,20}$");
    /** ASCII digits only: Integer.parseInt would also accept "+100" and non-ASCII digits. */
    private static final Pattern WHOLE_NUMBER_PATTERN = Pattern.compile("^[0-9]+$");
    private static final Pattern LEADING_ZEROS_PATTERN = Pattern.compile("^0+(?=.)");

    private static final int MIN_DEBIT_AMOUNT = 1;
    private static final int MAX_DEBIT_AMOUNT = 10_000_000;
    private static final int MAX_DEBIT_AMOUNT_DIGITS = 8;

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^.{3,30}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^.{1,72}$");

    private InputValidation() {
    }

    public static boolean isMsisdn(String value) {
        return value != null && MSISDN_PATTERN.matcher(value).matches();
    }

    public static boolean isPin(String value) {
        return value != null && PIN_PATTERN.matcher(value).matches();
    }


    public static boolean isTxnRefNo(String value) {
        return value != null && TXN_REFNO_PATTERN.matcher(value).matches();
    }

    public static boolean isTxnReason(String value) {
        return value != null && !value.isBlank() && value.length() <= 100;
    }

    public static boolean isDebitAmount(int value) {
        return value >= MIN_DEBIT_AMOUNT && value <= MAX_DEBIT_AMOUNT;
    }

    /**
     * Reads the amountPiasters of a debit: a whole number from 1 to 10000000, written with ASCII digits
     * only and not trimmed. Anything else (letters, a sign, a decimal point, spaces, too big) is empty.
     */
    public static OptionalInt parseDebitAmount(String value) {
        if (value == null || !WHOLE_NUMBER_PATTERN.matcher(value).matches()) {
            return OptionalInt.empty();
        }
        String significantDigits = LEADING_ZEROS_PATTERN.matcher(value).replaceFirst("");
        if (significantDigits.length() > MAX_DEBIT_AMOUNT_DIGITS) {
            return OptionalInt.empty();
        }
        int amount = Integer.parseInt(significantDigits);
        return isDebitAmount(amount) ? OptionalInt.of(amount) : OptionalInt.empty();
    }
    public static boolean isUsername(String value) {
        return value != null && USERNAME_PATTERN.matcher(value).matches();
    }

    public static boolean isPassword(String value) {
        return value != null && PASSWORD_PATTERN.matcher(value).matches();
    }
}
