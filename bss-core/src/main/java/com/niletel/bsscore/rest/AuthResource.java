package com.niletel.bsscore.rest;

import com.niletel.bsscore.common.ApiResponse;
import com.niletel.bsscore.common.JsonKeys;
import com.niletel.bsscore.common.SessionSupport;
import com.niletel.bsscore.filter.AccessRules;
import com.niletel.bsscore.model.AuthenticatedSubscriber;
import com.niletel.bsscore.service.AuthService;
import jakarta.ejb.EJB;
import jakarta.json.Json;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
public class AuthResource {

    @EJB
    private AuthService authService;

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response login(
            @FormParam("msisdn") String msisdn,
            @FormParam("pin") String pin,
            @Context HttpServletRequest request) {

        AuthenticatedSubscriber subscriber = authService.loginSubscriber(msisdn, pin);
        SessionSupport.startSession(request, subscriber.msisdn(), AccessRules.SUB);

        return ApiResponse.ok(Json.createObjectBuilder()
                .add(JsonKeys.MSISDN, subscriber.msisdn())
                .add(JsonKeys.FULL_NAME, subscriber.fullName())
                .add(JsonKeys.ROLE_CODE, AccessRules.SUB)
                .build());
    }
}
