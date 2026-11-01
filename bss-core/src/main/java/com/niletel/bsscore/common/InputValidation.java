package com.niletel.bsscore.common;

import java.util.regex.Pattern;

public final class InputValidation {

    private static final Pattern MSISDN_PATTERN = Pattern.compile("^01[0125][0-9]{8}$");
    private static final Pattern PIN_PATTERN = Pattern.compile("^[0-9]{4}$");
    private static final Pattern TXN_REFNO_PATTERN = Pattern.compile("^[A-Z0-9-]{6,20}$");

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
        return value >= 1 && value <= 10000000;
    }
}
