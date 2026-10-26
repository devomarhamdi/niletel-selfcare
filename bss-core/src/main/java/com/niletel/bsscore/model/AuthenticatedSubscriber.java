package com.niletel.bsscore.model;

public record AuthenticatedSubscriber(
        String msisdn,
        String fullName
) {}