package com.niletel.bsscore.rest;

import com.niletel.bsscore.common.BusinessException;
import com.niletel.bsscore.common.ErrorCode;
import com.niletel.bsscore.model.SubscriberResponse;
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
}
