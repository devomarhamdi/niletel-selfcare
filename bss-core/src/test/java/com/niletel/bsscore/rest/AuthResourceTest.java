package com.niletel.bsscore.rest;

import com.niletel.bsscore.common.BusinessException;
import com.niletel.bsscore.common.ErrorCode;
import com.niletel.bsscore.model.AuthenticatedSubscriber;
import com.niletel.bsscore.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthResourceTest {

    @Test
    void loginRespondsWithContractDataAndStartsSubscriberSession() throws Exception {
        AuthService authService = mock(AuthService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(null);
        when(request.getSession(true)).thenReturn(session);
        when(authService.loginSubscriber("01012345678", "1234"))
                .thenReturn(new AuthenticatedSubscriber("01012345678", "Mona Adel"));

        AuthResource resource = new AuthResource();
        Field field = AuthResource.class.getDeclaredField("authService");
        field.setAccessible(true);
        field.set(resource, authService);

        Response response = resource.login("01012345678", "1234", request);

        assertEquals(200, response.getStatus());
        assertEquals(
                "{\"RESULT\":\"OK\",\"DATA\":{\"MSISDN\":\"01012345678\",\"FULL_NAME\":\"Mona Adel\",\"ROLE_CODE\":\"SUB\"}}",
                response.getEntity()
        );
        verify(session).setAttribute("bss.user", "01012345678");
        verify(session).setAttribute("bss.role", "SUB");
    }

    @Test
    void failedLoginDoesNotCreateOrInvalidateSession() throws Exception {
        AuthService authService = mock(AuthService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(authService.loginSubscriber("01012345678", "0000"))
                .thenThrow(new BusinessException(ErrorCode.AUTH001));

        AuthResource resource = new AuthResource();
        Field field = AuthResource.class.getDeclaredField("authService");
        field.setAccessible(true);
        field.set(resource, authService);

        assertThrows(
                BusinessException.class,
                () -> resource.login("01012345678", "0000", request)
        );

        verify(request, never()).getSession(false);
        verify(request, never()).getSession(true);
    }
}
