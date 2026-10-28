package com.niletel.bsscore.common;

import com.niletel.bsscore.model.SubscriberRecord;
import com.niletel.bsscore.model.SubscriberResponse;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.time.LocalDate;

public final class SubscriberJson {

    private SubscriberJson() {
    }

    public static JsonObject profileData(SubscriberResponse subscriber) {
        return profileData(
                subscriber.msisdn(),
                subscriber.fullName(),
                subscriber.status(),
                subscriber.tariffCode(),
                subscriber.tariffName(),
                subscriber.balancePiasters(),
                subscriber.activationDate()
        );
    }

    public static JsonObject profileData(SubscriberRecord subscriber) {
        return profileData(
                subscriber.msisdn(),
                subscriber.fullName(),
                subscriber.status(),
                subscriber.tariffCode(),
                subscriber.tariffName(),
                subscriber.balancePiasters(),
                subscriber.activationDate()
        );
    }

    private static JsonObject profileData(
            String msisdn,
            String fullName,
            String status,
            String tariffCode,
            String tariffName,
            int balancePiasters,
            LocalDate activationDate
    ) {
        return Json.createObjectBuilder()
                .add(JsonKeys.MSISDN, msisdn)
                .add(JsonKeys.FULL_NAME, fullName)
                .add(JsonKeys.STATUS, status)
                .add(JsonKeys.TARIFF_CODE, tariffCode)
                .add(JsonKeys.TARIFF_NAME, tariffName)
                .add(JsonKeys.BALANCE_PIASTERS, balancePiasters)
                .add(JsonKeys.ACTIVATION_DT, LegacyFormats.formatDate(activationDate))
                .build();
    }
}
