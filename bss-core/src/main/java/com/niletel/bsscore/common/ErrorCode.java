package com.niletel.bsscore.common;

/** Every error the API can answer: code text, HTTP status (C4) and a default English message (logs only). */
public enum ErrorCode {

    SES001("SES001", 401, "Session missing or expired"),
    SEC001("SEC001", 200, "Access denied"),
    VAL001("VAL001", 200, "Invalid or missing parameter"),
    SYS999("SYS999", 500, "Unexpected error"),

    AUTH001("AUTH001", 200, "Invalid credentials"),
    AUTH002("AUTH002", 200, "Account locked"),
    AUTH003("AUTH003", 200, "Operator authentication failed"),

    VCH001("VCH001", 200, "Voucher not found"),
    VCH002("VCH002", 200, "Voucher already used"),
    BAL001("BAL001", 200, "Invalid amount"),
    BAL002("BAL002", 200, "Insufficient balance"),

    SUB002("SUB002", 200, "Subscriber not active"),
    SUB404("SUB404", 200, "Subscriber not found"),
    TXN002("TXN002", 200, "Reference number already used"),
    TXN003("TXN003", 200, "Transaction already refunded"),
    TXN404("TXN404", 200, "Transaction not found");

    private final String code;
    private final int httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, int httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public String code() {
        return code;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
