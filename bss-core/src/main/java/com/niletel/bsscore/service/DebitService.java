package com.niletel.bsscore.service;

import com.niletel.bsscore.common.BusinessException;
import com.niletel.bsscore.common.DataAccessException;
import com.niletel.bsscore.common.ErrorCode;
import com.niletel.bsscore.dao.LedgerDao;
import com.niletel.bsscore.dao.SubscriberDao;
import com.niletel.bsscore.model.BalanceSnapshot;
import com.niletel.bsscore.model.DebitResult;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

import java.sql.SQLException;

/**
 * Takes money out of a balance and writes the ledger row, in one transaction.
 *
 * <p>Order of the checks (Interface Contract C12, the first failure wins): VAL001 and BAL001 are decided before
 * this bean is called (resource); here SUB002 (line suspended), TXN002 (reference already used by any
 * subscriber) and BAL002 (insufficient balance) follow in this order. The status and the balance are read
 * with the row lock held, never from the session, so a line suspended while its owner is logged in is
 * refused at the next debit.
 */
@Stateless
public class DebitService {

    @Inject
    private SubscriberDao subscriberDao;

    @Inject
    private LedgerDao ledgerDao;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public DebitResult debit(String msisdn, int amountPiasters, String refNo, String reason) {
        try {
            BalanceSnapshot subscriber = subscriberDao.lockBalance(msisdn)
                    .orElseThrow(() -> new BusinessException(ErrorCode.SYS999));

            if ("S".equals(subscriber.status())) {
                throw new BusinessException(ErrorCode.SUB002);
            }
            if (ledgerDao.debitReferenceExists(refNo)) {
                throw new BusinessException(ErrorCode.TXN002);
            }
            if (subscriber.balancePiasters() < amountPiasters) {
                throw new BusinessException(ErrorCode.BAL002);
            }

            int balanceAfter = subscriberDao.deductBalance(msisdn, amountPiasters);
            long txnId = ledgerDao.insertDebit(msisdn, amountPiasters, balanceAfter, refNo, reason)
                    .orElseThrow(() -> new BusinessException(ErrorCode.TXN002));

            return new DebitResult(txnId, balanceAfter);

        } catch (SQLException e) {
            throw new DataAccessException("Database operation failed", e);
        }
    }
}
