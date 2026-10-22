package com.niletel.bsscore.model;

public record TariffRecord(
        String tariffCode,
        String tariffName,
        int ratePerMinPiasters,
        String activeFlag
) {}
