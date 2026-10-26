package com.niletel.bsscore.common;

import com.niletel.bsscore.model.SubscriberRecord;
import com.niletel.bsscore.model.SubscriberResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubscriberJsonTest {
    private static final String MONA_PROFILE =
            "{\"MSISDN\":\"01012345678\",\"FULL_NAME\":\"Mona Adel\",\"STATUS\":\"A\","
                    + "\"TARIFF_CODE\":\"PRE01\",\"TARIFF_NAME\":\"Nile Prepaid\","
                    + "\"BALANCE_PIASTERS\":6750,\"ACTIVATION_DT\":\"01/03/2025\"}";

    @Test
    void mapsSubscriberResponseToExactProfileData() {
        SubscriberResponse subscriber = new SubscriberResponse(
                "01012345678",
                "Mona Adel",
                "A",
                6750,
                LocalDate.of(2025, 3, 1),
                "PRE01",
                "Nile Prepaid"
        );

        assertEquals(MONA_PROFILE, SubscriberJson.profileData(subscriber).toString());
    }

    @Test
    void mapsSubscriberRecordWithoutExposingPinHash() {
        SubscriberRecord subscriber = new SubscriberRecord(
                "01012345678",
                "Mona Adel",
                "secret-pin-hash",
                "A",
                6750,
                LocalDate.of(2025, 3, 1),
                "PRE01",
                "Nile Prepaid"
        );

        assertEquals(MONA_PROFILE, SubscriberJson.profileData(subscriber).toString());
    }

    @Test
    void preservesZeroBalanceAndFormatsOldActivationDate() {
        SubscriberResponse subscriber = new SubscriberResponse(
                "01512345678",
                "Youssef Fathy",
                "A",
                0,
                LocalDate.of(2016, 8, 12),
                "OLD99",
                "Classic 2015"
        );

        assertEquals(
                "{\"MSISDN\":\"01512345678\",\"FULL_NAME\":\"Youssef Fathy\",\"STATUS\":\"A\","
                        + "\"TARIFF_CODE\":\"OLD99\",\"TARIFF_NAME\":\"Classic 2015\","
                        + "\"BALANCE_PIASTERS\":0,\"ACTIVATION_DT\":\"12/08/2016\"}",
                SubscriberJson.profileData(subscriber).toString()
        );
    }
}
