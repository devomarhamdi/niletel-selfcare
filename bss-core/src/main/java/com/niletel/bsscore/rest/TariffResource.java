package com.niletel.bsscore.rest;

import com.niletel.bsscore.common.ApiResponse;
import com.niletel.bsscore.model.TariffRecord;
import com.niletel.bsscore.service.TariffCache;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/tariffs")
@Produces(MediaType.APPLICATION_JSON)
public class TariffResource {

    @Inject
    private TariffCache tariffCache;

    @GET
    public Response getTariffs() {
        List<TariffRecord> tariffs = tariffCache.getAll();

        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (TariffRecord tariff : tariffs) {
            arrayBuilder.add(Json.createObjectBuilder()
                    .add("TARIFF_CODE", tariff.tariffCode())
                    .add("TARIFF_NAME", tariff.tariffName())
                    .add("RATE_PER_MIN_PIASTERS", tariff.ratePerMinPiasters())
                    .add("ACTIVE_FLAG", tariff.activeFlag())
            );
        }

        return ApiResponse.ok(arrayBuilder.build());
    }
}