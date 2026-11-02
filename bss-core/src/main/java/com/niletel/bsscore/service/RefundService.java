package com.niletel.bsscore.service;

import com.niletel.bsscore.common.BusinessException;
import com.niletel.bsscore.common.DataAccessException;
import com.niletel.bsscore.common.ErrorCode;
import com.niletel.bsscore.dao.LedgerDao;
import com.niletel.bsscore.dao.SubscriberDao;
import com.niletel.bsscore.model.RefundResult;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

import java.sql.SQLException;

/** Gives back the full amount of one debit, once, and only to the subscriber who was debited. */
@Stateless
public class RefundService {

    private static final String REFUND_REASON = "REFUND";

    @Inject
    private SubscriberDao subscriberDao;

    @Inject
    private LedgerDao ledgerDao;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public RefundResult refund(String msisdn, String refNo) {
        try {
            // Serializes every money operation of this line. The status is deliberately not checked:
            // a suspended line can still receive a refund (business rule 5).
            subscriberDao.lockBalance(msisdn)
                    .orElseThrow(() -> new BusinessException(ErrorCode.SYS999));

            int amountPiasters = ledgerDao.findDebitAmount(msisdn, refNo)
                    .orElseThrow(() -> new BusinessException(ErrorCode.TXN404));
            if (ledgerDao.refundReferenceExists(refNo)) {
                throw new BusinessException(ErrorCode.TXN003);
            }

            int balanceAfter = subscriberDao.creditBalance(msisdn, amountPiasters);
            long txnId = ledgerDao.insertRefund(msisdn, amountPiasters, balanceAfter, refNo, REFUND_REASON)
                    .orElseThrow(() -> new BusinessException(ErrorCode.TXN003));
            // L-16: the SMS for this balance change is sent here, inside the same transaction.

            return new RefundResult(txnId, amountPiasters, balanceAfter);

        } catch (SQLException e) {
            throw new DataAccessException("Database operation failed", e);
        }
    }
}
