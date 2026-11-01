package com.niletel.bsscore.rest;

import com.niletel.bsscore.common.*;
import com.niletel.bsscore.model.DebitResult;
import com.niletel.bsscore.model.SubscriberResponse;
import com.niletel.bsscore.service.DebitService;
import com.niletel.bsscore.service.SubscriberService;
import jakarta.ejb.EJB;
import jakarta.json.Json;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/subscriber/me")
public class SubscriberResource {

    @EJB
    private SubscriberService subscriberService;

    @EJB
    private DebitService debitService;

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

    @POST
    @Path("/debit")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response debit(
            @FormParam("amountPiasters") String amountPiasters,
            @FormParam("refNo") String refNo,
            @FormParam("reason") String reason,
            @Context HttpServletRequest request) {

        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new BusinessException(ErrorCode.SES001);
        }

        Object user =  session.getAttribute(SessionKeys.USER);
        if (!(user instanceof String msisdn) || msisdn.isBlank()) {
            throw new BusinessException(ErrorCode.SES001);
        }

        if (amountPiasters == null || amountPiasters.isBlank()
            || !InputValidation.isTxnRefNo(refNo)
            || !InputValidation.isTxnReason(reason)) {
            throw new BusinessException(ErrorCode.VAL001);
        }

        final int parsedAmount;
        try {
            parsedAmount = Integer.parseInt(amountPiasters);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.BAL001);
        }
        if (!InputValidation.isDebitAmount(parsedAmount)) {
            throw new BusinessException(ErrorCode.BAL001);
        }

        DebitResult result = debitService.debit(msisdn, parsedAmount, refNo, reason);
        return ApiResponse.ok(Json.createObjectBuilder()
                .add(JsonKeys.TXN_ID, result.txnId())
                .add(JsonKeys.BALANCE_PIASTERS, result.balanceAfter())
                .build());
    }
}
