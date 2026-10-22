package com.niletel.bsscore.model;

public record LedgerRecord(
        long txnId,
        String msisdn,
        String txnType,
        int amountPiasters,
        int balanceAfter,
        String refNo,
        String reason
) {}