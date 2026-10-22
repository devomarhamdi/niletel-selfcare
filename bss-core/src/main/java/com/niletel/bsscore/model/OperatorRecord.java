package com.niletel.bsscore.model;

public record OperatorRecord(
        String username,
        String passwordHash,
        String fullName,
        String roleCode,
        boolean active
) {}
