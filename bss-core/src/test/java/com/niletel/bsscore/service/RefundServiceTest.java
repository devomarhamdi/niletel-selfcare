package com.niletel.bsscore.service;

import com.niletel.bsscore.common.BusinessException;
import com.niletel.bsscore.common.ErrorCode;
import com.niletel.bsscore.dao.LedgerDao;
import com.niletel.bsscore.dao.SubscriberDao;
import com.niletel.bsscore.model.BalanceSnapshot;
import com.niletel.bsscore.model.RefundResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefundServiceTest {
    private static final String MSISDN = "01012345678";
    private static final String REF_NO = "SC-20261012-000015";

    private final SubscriberDao subscriberDao = mock(SubscriberDao.class);
    private final LedgerDao ledgerDao = mock(LedgerDao.class);
    private final RefundService service = new RefundService();

    @BeforeEach
    void setUp() throws Exception {
        setField("subscriberDao", subscriberDao);
        setField("ledgerDao", ledgerDao);
    }

    @Test
    void locksLineThenCreditsFullDebitAmountAndInsertsRefundRow() throws Exception {
        when(subscriberDao.lockBalance(MSISDN)).thenReturn(Optional.of(new BalanceSnapshot("A", 3250)));
        when(ledgerDao.findDebitAmount(MSISDN, REF_NO)).thenReturn(OptionalInt.of(3500));
        when(ledgerDao.refundReferenceExists(REF_NO)).thenReturn(false);
        when(subscriberDao.creditBalance(MSISDN, 3500)).thenReturn(6750);
        when(ledgerDao.insertRefund(MSISDN, 3500, 6750, REF_NO, "REFUND"))
                .thenReturn(OptionalLong.of(50232L));

        RefundResult result = service.refund(MSISDN, REF_NO);

        assertEquals(new RefundResult(50232L, 3500, 6750), result);
        InOrder order = inOrder(subscriberDao, ledgerDao);
        order.verify(subscriberDao).lockBalance(MSISDN);
        order.verify(ledgerDao).findDebitAmount(MSISDN, REF_NO);
        order.verify(ledgerDao).refundReferenceExists(REF_NO);
        order.verify(subscriberDao).creditBalance(MSISDN, 3500);
        order.verify(ledgerDao).insertRefund(MSISDN, 3500, 6750, REF_NO, "REFUND");
    }

    @Test
    void refundRowUsesTheBalanceReturnedByTheUpdateNotAJavaCalculation() throws Exception {
        when(subscriberDao.lockBalance(MSISDN)).thenReturn(Optional.of(new BalanceSnapshot("A", 100)));
        when(ledgerDao.findDebitAmount(MSISDN, REF_NO)).thenReturn(OptionalInt.of(500));
        when(subscriberDao.creditBalance(MSISDN, 500)).thenReturn(9999);
        when(ledgerDao.insertRefund(MSISDN, 500, 9999, REF_NO, "REFUND"))
                .thenReturn(OptionalLong.of(50240L));

        assertEquals(new RefundResult(50240L, 500, 9999), service.refund(MSISDN, REF_NO));
    }

    @Test
    void unknownReferenceOfThisSubscriberIsTxn404AndChangesNothing() throws Exception {
        when(subscriberDao.lockBalance(MSISDN)).thenReturn(Optional.of(new BalanceSnapshot("A", 3250)));
        when(ledgerDao.findDebitAmount(MSISDN, REF_NO)).thenReturn(OptionalInt.empty());

        BusinessException error = assertThrows(BusinessException.class, () -> service.refund(MSISDN, REF_NO));

        assertEquals(ErrorCode.TXN404, error.getErrorCode());
        verify(ledgerDao, never()).refundReferenceExists(REF_NO);
        verify(subscriberDao, never()).creditBalance(MSISDN, 3500);
        verify(ledgerDao, never()).insertRefund(MSISDN, 3500, 6750, REF_NO, "REFUND");
    }

    @Test
    void debitOfAnotherSubscriberIsTxn404BecauseTheLookupIsScopedToTheCaller() throws Exception {
        when(subscriberDao.lockBalance("01123456789")).thenReturn(Optional.of(new BalanceSnapshot("A", 500)));
        when(ledgerDao.findDebitAmount("01123456789", REF_NO)).thenReturn(OptionalInt.empty());

        BusinessException error = assertThrows(
                BusinessException.class, () -> service.refund("01123456789", REF_NO));

        assertEquals(ErrorCode.TXN404, error.getErrorCode());
        verify(ledgerDao).findDebitAmount("01123456789", REF_NO);
        verify(subscriberDao, never()).creditBalance("01123456789", 3500);
    }

    @Test
    void alreadyRefundedDebitIsTxn003BeforeAnyMoneyMoves() throws Exception {
        when(subscriberDao.lockBalance(MSISDN)).thenReturn(Optional.of(new BalanceSnapshot("A", 6750)));
        when(ledgerDao.findDebitAmount(MSISDN, "SC-20261009-000007")).thenReturn(OptionalInt.of(4500));
        when(ledgerDao.refundReferenceExists("SC-20261009-000007")).thenReturn(true);

        BusinessException error = assertThrows(
                BusinessException.class, () -> service.refund(MSISDN, "SC-20261009-000007"));

        assertEquals(ErrorCode.TXN003, error.getErrorCode());
        verify(subscriberDao, never()).creditBalance(MSISDN, 4500);
        verify(ledgerDao, never()).insertRefund(MSISDN, 4500, 11250, "SC-20261009-000007", "REFUND");
    }

    @Test
    void unknownReferenceIsCheckedBeforeTheAlreadyRefundedRule() throws Exception {
        when(subscriberDao.lockBalance(MSISDN)).thenReturn(Optional.of(new BalanceSnapshot("A", 3250)));
        when(ledgerDao.findDebitAmount(MSISDN, REF_NO)).thenReturn(OptionalInt.empty());
        when(ledgerDao.refundReferenceExists(REF_NO)).thenReturn(true);

        BusinessException error = assertThrows(BusinessException.class, () -> service.refund(MSISDN, REF_NO));

        assertEquals(ErrorCode.TXN404, error.getErrorCode());
    }

    @Test
    void simultaneousRefundLosingTheUniqueIndexRaceIsTxn003() throws Exception {
        when(subscriberDao.lockBalance(MSISDN)).thenReturn(Optional.of(new BalanceSnapshot("A", 3250)));
        when(ledgerDao.findDebitAmount(MSISDN, REF_NO)).thenReturn(OptionalInt.of(3500));
        when(ledgerDao.refundReferenceExists(REF_NO)).thenReturn(false);
        when(subscriberDao.creditBalance(MSISDN, 3500)).thenReturn(6750);
        when(ledgerDao.insertRefund(MSISDN, 3500, 6750, REF_NO, "REFUND")).thenReturn(OptionalLong.empty());

        BusinessException error = assertThrows(BusinessException.class, () -> service.refund(MSISDN, REF_NO));

        assertEquals(ErrorCode.TXN003, error.getErrorCode());
    }

    private void setField(String name, Object value) throws ReflectiveOperationException {
        var field = RefundService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }
}
