package com.niletel.bsscore.model;

import java.time.LocalDate;

public record SubscriberResponse(
        String msisdn,
        String fullName,
        String status,
        int balancePiasters,
        LocalDate activationDate,
        String tariffCode,
        String tariffName
) {}
