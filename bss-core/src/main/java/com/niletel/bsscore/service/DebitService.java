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
