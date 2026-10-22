package com.niletel.bsscore.model;

import java.time.LocalDate;

public record SubscriberRecord(
        String msisdn,
        String fullName,
        String pinHash,
        String status,
        int balancePiasters,
        LocalDate activationDate,
        String tariffCode,
        String tariffName
) {}
