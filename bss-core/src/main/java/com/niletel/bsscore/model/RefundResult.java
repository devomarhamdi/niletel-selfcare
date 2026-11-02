package com.niletel.bsscore.model;

/** Outcome of a refund: the new ledger row, the amount given back and the balance after the refund. */
public record RefundResult(long txnId, int amountPiasters, int balanceAfter) {}
