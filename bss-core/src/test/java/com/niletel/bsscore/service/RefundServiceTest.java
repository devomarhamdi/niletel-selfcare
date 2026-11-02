package com.niletel.bsscore.service;

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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
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

    private void setField(String name, Object value) throws ReflectiveOperationException {
        var field = RefundService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }
}
