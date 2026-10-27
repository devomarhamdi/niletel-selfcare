package com.niletel.bsscore.service;

import com.niletel.bsscore.common.BusinessException;
import com.niletel.bsscore.common.ErrorCode;
import com.niletel.bsscore.common.PasswordHasher;
import com.niletel.bsscore.dao.SubscriberDao;
import com.niletel.bsscore.model.AuthenticatedSubscriber;
import com.niletel.bsscore.model.SubscriberRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    private final SubscriberDao subscriberDao = mock(SubscriberDao.class);
    private final AuthService authService = new AuthService();

    @BeforeEach
    void setUp() throws Exception {
        Field field = AuthService.class.getDeclaredField("subscriberDao");
        field.setAccessible(true);
        field.set(authService, subscriberDao);
    }

    @Test
    void rejectsInvalidInputBeforeLookingUpSubscriber() throws Exception {
        BusinessException error = assertThrows(
                BusinessException.class,
                () -> authService.loginSubscriber("01012345678", "12a4")
        );

        assertEquals(ErrorCode.VAL001, error.getErrorCode());
        verify(subscriberDao, never()).findByMsisdn("01012345678");
    }

    @Test
    void returnsAuthenticatedSubscriberAfterValidCredentials() throws Exception {
        when(subscriberDao.findByMsisdn("01012345678"))
                .thenReturn(Optional.of(subscriber("01012345678", "Mona Adel", "A", "1234")));

        assertEquals(
                new AuthenticatedSubscriber("01012345678", "Mona Adel"),
                authService.loginSubscriber("01012345678", "1234")
        );
    }

    @Test
    void usesAuth001ForUnknownSubscriberAndWrongPin() throws Exception {
        when(subscriberDao.findByMsisdn("01099999999")).thenReturn(Optional.empty());
        when(subscriberDao.findByMsisdn("01012345678"))
                .thenReturn(Optional.of(subscriber("01012345678", "Mona Adel", "A", "1234")));

        BusinessException unknown = assertThrows(
                BusinessException.class,
                () -> authService.loginSubscriber("01099999999", "1234")
        );
        BusinessException wrongPin = assertThrows(
                BusinessException.class,
                () -> authService.loginSubscriber("01012345678", "0000")
        );

        assertEquals(ErrorCode.AUTH001, unknown.getErrorCode());
        assertEquals(unknown.getMessage(), wrongPin.getMessage());
    }

    @Test
    void checksSuspendedStatusOnlyAfterPinMatches() throws Exception {
        when(subscriberDao.findByMsisdn("01234567890"))
                .thenReturn(Optional.of(subscriber("01234567890", "Salma Nabil", "S", "1111")));

        BusinessException wrongPin = assertThrows(
                BusinessException.class,
                () -> authService.loginSubscriber("01234567890", "0000")
        );
        BusinessException correctPin = assertThrows(
                BusinessException.class,
                () -> authService.loginSubscriber("01234567890", "1111")
        );

        assertEquals(ErrorCode.AUTH001, wrongPin.getErrorCode());
        assertEquals(ErrorCode.AUTH002, correctPin.getErrorCode());
    }

    private static SubscriberRecord subscriber(String msisdn, String fullName, String status, String pin) {
        return new SubscriberRecord(
                msisdn,
                fullName,
                PasswordHasher.sha256Hex(pin),
                status,
                0,
                LocalDate.of(2025, 1, 1),
                "PRE01",
                "Nile Prepaid"
        );
    }
}
