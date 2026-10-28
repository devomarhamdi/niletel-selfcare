package com.niletel.bsscore.rest;

import com.niletel.bsscore.common.ApiResponse;
import com.niletel.bsscore.common.BusinessException;
import com.niletel.bsscore.common.ErrorCode;
import com.niletel.bsscore.common.SessionKeys;
import com.niletel.bsscore.common.SubscriberJson;
import com.niletel.bsscore.model.SubscriberResponse;
import com.niletel.bsscore.service.SubscriberService;
import jakarta.ejb.EJB;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

@Path("/subscriber/me")
public class SubscriberResource {

    @EJB
    private SubscriberService subscriberService;

    @GET
    public Response getProfile(@Context HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            throw new BusinessException(ErrorCode.SES001);
        }

        Object user =  session.getAttribute(SessionKeys.USER);

        if (!(user instanceof String msisdn) || msisdn.isBlank()) {
            throw new BusinessException(ErrorCode.SES001);
        }

        SubscriberResponse subscriber = subscriberService.getProfile(msisdn);

        return ApiResponse.ok(SubscriberJson.profileData(subscriber));
    }
}
