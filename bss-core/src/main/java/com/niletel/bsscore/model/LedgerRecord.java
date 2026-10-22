package com.niletel.bsscore.model;

import java.time.LocalDateTime;

public record LedgerRecord(
        long txnId,
        String msisdn,
        LocalDateTime txnTs,
        String txnType,
        int amountPiasters,
        int balanceAfter,
        String refNo,
        String reason
) {}
