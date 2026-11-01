package com.niletel.bsscore.rest;

import com.niletel.bsscore.common.BusinessException;
import com.niletel.bsscore.common.ErrorCode;
import com.niletel.bsscore.model.DebitResult;
import com.niletel.bsscore.model.SubscriberResponse;
import com.niletel.bsscore.service.DebitService;
import com.niletel.bsscore.service.SubscriberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubscriberResourceTest {

    @Test
    void profileUsesOnlySessionIdentity() throws Exception {
        SubscriberService subscriberService = mock(SubscriberService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("bss.user")).thenReturn("01012345678");
        when(subscriberService.getProfile("01012345678")).thenReturn(new SubscriberResponse(
                "01012345678", "Mona Adel", "A", 6750,
                LocalDate.of(2025, 3, 1), "PRE01", "Nile Prepaid"
        ));

        SubscriberResource resource = new SubscriberResource();
        Field field = SubscriberResource.class.getDeclaredField("subscriberService");
        field.setAccessible(true);
        field.set(resource, subscriberService);

        Response response = resource.getProfile(request);

        assertEquals(200, response.getStatus());
        assertEquals(
                "{\"RESULT\":\"OK\",\"DATA\":{\"MSISDN\":\"01012345678\","
                        + "\"FULL_NAME\":\"Mona Adel\",\"STATUS\":\"A\","
                        + "\"TARIFF_CODE\":\"PRE01\",\"TARIFF_NAME\":\"Nile Prepaid\","
                        + "\"BALANCE_PIASTERS\":6750,\"ACTIVATION_DT\":\"01/03/2025\"}}",
                response.getEntity()
        );
        verify(subscriberService).getProfile("01012345678");
    }

    @Test
    void missingSessionIdentityDoesNotCallService() throws Exception {
        SubscriberService subscriberService = mock(SubscriberService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(null);

        SubscriberResource resource = new SubscriberResource();
        Field field = SubscriberResource.class.getDeclaredField("subscriberService");
        field.setAccessible(true);
        field.set(resource, subscriberService);

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> resource.getProfile(request)
        );

        assertEquals(ErrorCode.SES001, error.getErrorCode());
        verify(subscriberService, never()).getProfile("01012345678");
    }

    @Test
    void debitReturnsLedgerIdAndBalanceAfterDebit() throws Exception {
        DebitService debitService = mock(DebitService.class);
        HttpServletRequest request = debitRequest("01012345678");
        when(debitService.debit("01012345678", 250, "REF-001", "Call charge"))
                .thenReturn(new DebitResult(50231L, 750));

        SubscriberResource resource = new SubscriberResource();
        setField(resource, "debitService", debitService);

        Response response = resource.debit("250", "REF-001", "Call charge", request);

        assertEquals(200, response.getStatus());
        assertEquals(
                "{\"RESULT\":\"OK\",\"DATA\":{\"TXN_ID\":50231,\"BALANCE_PIASTERS\":750}}",
                response.getEntity()
        );
        verify(debitService).debit("01012345678", 250, "REF-001", "Call charge");
    }

    @Test
    void missingOrInvalidDebitFieldsReturnValidationErrorBeforeServiceCall() throws Exception {
        DebitService debitService = mock(DebitService.class);
        SubscriberResource resource = new SubscriberResource();
        setField(resource, "debitService", debitService);

        BusinessException missingReason = assertThrows(
                BusinessException.class,
                () -> resource.debit("250", "REF-001", null, debitRequest("01012345678"))
        );
        BusinessException invalidReference = assertThrows(
                BusinessException.class,
                () -> resource.debit(
                        "250", "ab", "Call charge", debitRequest("01012345678")
                )
        );
        BusinessException emptyAmount = assertThrows(
                BusinessException.class,
                () -> resource.debit(
                        "", "REF-001", "Call charge", debitRequest("01012345678")
                )
        );

        assertEquals(ErrorCode.VAL001, missingReason.getErrorCode());
        assertEquals(ErrorCode.VAL001, invalidReference.getErrorCode());
        assertEquals(ErrorCode.VAL001, emptyAmount.getErrorCode());
        verify(debitService, never()).debit("01012345678", 250, "REF-001", "Call charge");
    }

    @Test
    void nonNumericDebitAmountReturnsBal001BeforeServiceCall() throws Exception {
        DebitService debitService = mock(DebitService.class);
        SubscriberResource resource = new SubscriberResource();
        setField(resource, "debitService", debitService);

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> resource.debit(
                        "not-a-number", "REF-001", "Call charge",
                        debitRequest("01012345678")
                )
        );

        assertEquals(ErrorCode.BAL001, error.getErrorCode());
        verify(debitService, never()).debit(
                "01012345678", 0, "REF-001", "Call charge"
        );
    }

    private static HttpServletRequest debitRequest(String msisdn) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("bss.user")).thenReturn(msisdn);
        return request;
    }

    private static void setField(Object target, String fieldName, Object value)
            throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
