package com.niletel.bsscore.service;

import com.niletel.bsscore.common.BusinessException;
import com.niletel.bsscore.common.DataAccessException;
import com.niletel.bsscore.common.ErrorCode;
import com.niletel.bsscore.dao.LedgerDao;
import com.niletel.bsscore.dao.SubscriberDao;
import com.niletel.bsscore.model.BalanceSnapshot;
import com.niletel.bsscore.model.DebitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.sql.SQLException;
import java.util.Optional;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DebitServiceTest {
    private final SubscriberDao subscriberDao = mock(SubscriberDao.class);
    private final LedgerDao ledgerDao = mock(LedgerDao.class);
    private final DebitService service = new DebitService();

    @BeforeEach
    void setUp() throws Exception {
        var subscriberDaoField = DebitService.class.getDeclaredField("subscriberDao");
        subscriberDaoField.setAccessible(true);
        subscriberDaoField.set(service, subscriberDao);

        var ledgerDaoField = DebitService.class.getDeclaredField("ledgerDao");
        ledgerDaoField.setAccessible(true);
        ledgerDaoField.set(service, ledgerDao);
    }

    @Test
    void locksThenDeductsAndInsertsLedgerWithReturnedBalance() throws Exception {
        when(subscriberDao.lockBalance("01012345678"))
                .thenReturn(Optional.of(new BalanceSnapshot("A", 1000)));
        when(subscriberDao.deductBalance("01012345678", 250)).thenReturn(750);
        when(ledgerDao.debitReferenceExists("REF-001")).thenReturn(false);
        when(ledgerDao.insertDebit("01012345678", 250, 750, "REF-001", "Call charge"))
                .thenReturn(OptionalLong.of(50231L));

        DebitResult result = service.debit(
                "01012345678", 250, "REF-001", "Call charge"
        );

        assertEquals(new DebitResult(50231L, 750), result);
        InOrder order = inOrder(subscriberDao, ledgerDao);
        order.verify(subscriberDao).lockBalance("01012345678");
        order.verify(ledgerDao).debitReferenceExists("REF-001");
        order.verify(subscriberDao).deductBalance("01012345678", 250);
        order.verify(ledgerDao).insertDebit(
                "01012345678", 250, 750, "REF-001", "Call charge"
        );
    }

    @Test
    void insufficientBalanceFailsBeforeChangingBalanceOrInsertingLedger() throws Exception {
        when(subscriberDao.lockBalance("01012345678"))
                .thenReturn(Optional.of(new BalanceSnapshot("A", 100)));

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.debit("01012345678", 250, "REF-001", "Call charge")
        );

        assertEquals(ErrorCode.BAL002, error.getErrorCode());
        verify(ledgerDao).debitReferenceExists("REF-001");
        verify(subscriberDao, never()).deductBalance("01012345678", 250);
        verify(ledgerDao, never()).insertDebit(
                "01012345678", 250, 0, "REF-001", "Call charge"
        );
    }

    @Test
    void inactiveSubscriberFailsBeforeChangingBalanceOrInsertingLedger() throws Exception {
        when(subscriberDao.lockBalance("01012345678"))
                .thenReturn(Optional.of(new BalanceSnapshot("S", 1000)));

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.debit("01012345678", 250, "REF-001", "Call charge")
        );

        assertEquals(ErrorCode.SUB002, error.getErrorCode());
        verify(ledgerDao, never()).debitReferenceExists("REF-001");
        verify(subscriberDao, never()).deductBalance("01012345678", 250);
        verify(ledgerDao, never()).insertDebit(
                "01012345678", 250, 0, "REF-001", "Call charge"
        );
    }

    @Test
    void usedReferenceFailsBeforeCheckingBalance() throws Exception {
        when(subscriberDao.lockBalance("01012345678"))
                .thenReturn(Optional.of(new BalanceSnapshot("A", 100)));
        when(ledgerDao.debitReferenceExists("REF-001")).thenReturn(true);

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.debit("01012345678", 250, "REF-001", "Call charge")
        );

        assertEquals(ErrorCode.TXN002, error.getErrorCode());
        verify(subscriberDao, never()).deductBalance("01012345678", 250);
        verify(ledgerDao, never()).insertDebit(
                "01012345678", 250, 0, "REF-001", "Call charge"
        );
    }

    @Test
    void concurrentDuplicateReferenceFailsAndRollsBackDebit() throws Exception {
        when(subscriberDao.lockBalance("01012345678"))
                .thenReturn(Optional.of(new BalanceSnapshot("A", 1000)));
        when(ledgerDao.debitReferenceExists("REF-001")).thenReturn(false);
        when(subscriberDao.deductBalance("01012345678", 250)).thenReturn(750);
        when(ledgerDao.insertDebit("01012345678", 250, 750, "REF-001", "Call charge"))
                .thenReturn(OptionalLong.empty());

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.debit("01012345678", 250, "REF-001", "Call charge")
        );

        assertEquals(ErrorCode.TXN002, error.getErrorCode());
    }

    @Test
    void ledgerInsertFailurePropagatesAfterBalanceUpdateForTransactionRollback() throws Exception {
        when(subscriberDao.lockBalance("01012345678"))
                .thenReturn(Optional.of(new BalanceSnapshot("A", 1000)));
        when(ledgerDao.debitReferenceExists("REF-001")).thenReturn(false);
        when(subscriberDao.deductBalance("01012345678", 250)).thenReturn(750);
        when(ledgerDao.insertDebit("01012345678", 250, 750, "REF-001", "Call charge"))
                .thenThrow(new SQLException("Forced ledger insert failure"));

        assertThrows(
                DataAccessException.class,
                () -> service.debit("01012345678", 250, "REF-001", "Call charge")
        );

        InOrder order = inOrder(subscriberDao, ledgerDao);
        order.verify(subscriberDao).lockBalance("01012345678");
        order.verify(ledgerDao).debitReferenceExists("REF-001");
        order.verify(subscriberDao).deductBalance("01012345678", 250);
        order.verify(ledgerDao).insertDebit(
                "01012345678", 250, 750, "REF-001", "Call charge"
        );
    }
}
