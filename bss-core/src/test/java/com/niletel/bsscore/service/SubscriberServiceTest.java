package com.niletel.bsscore.service;

import com.niletel.bsscore.common.BusinessException;
import com.niletel.bsscore.common.ErrorCode;
import com.niletel.bsscore.dao.SubscriberDao;
import com.niletel.bsscore.model.SubscriberRecord;
import com.niletel.bsscore.model.SubscriberResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubscriberServiceTest {
    private final SubscriberDao subscriberDao = mock(SubscriberDao.class);
    private final SubscriberService subscriberService = new SubscriberService();

    @BeforeEach
    void setUp() throws Exception {
        Field field = SubscriberService.class.getDeclaredField("subscriberDao");
        field.setAccessible(true);
        field.set(subscriberService, subscriberDao);
    }

    @Test
    void getsProfileWithOneDaoCallAndAllowsSuspendedSubscribers() throws Exception {
        SubscriberRecord suspendedSubscriber = new SubscriberRecord(
                "01234567890", "Salma Nabil", "hash", "S", 2500,
                LocalDate.of(2024, 1, 20), "PRE01", "Nile Prepaid"
        );
        when(subscriberDao.findByMsisdn("01234567890"))
                .thenReturn(Optional.of(suspendedSubscriber));

        assertEquals(
                new SubscriberResponse(
                        "01234567890", "Salma Nabil", "S", 2500,
                        LocalDate.of(2024, 1, 20), "PRE01", "Nile Prepaid"
                ),
                subscriberService.getProfile("01234567890")
        );
        verify(subscriberDao).findByMsisdn("01234567890");
    }

    @Test
    void missingSubscriberMapsToSys999() throws Exception {
        when(subscriberDao.findByMsisdn("01012345678")).thenReturn(Optional.empty());

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> subscriberService.getProfile("01012345678")
        );

        assertEquals(ErrorCode.SYS999, error.getErrorCode());
    }
}
