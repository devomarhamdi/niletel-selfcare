package com.niletel.bsscore.rest;

import com.niletel.bsscore.common.*;
import com.niletel.bsscore.model.DebitResult;
import com.niletel.bsscore.model.RefundResult;
import com.niletel.bsscore.model.SubscriberResponse;
import com.niletel.bsscore.service.DebitService;
import com.niletel.bsscore.service.RefundService;
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

    @EJB
    private RefundService refundService;

    @GET
    public Response getProfile(@Context HttpServletRequest request) {
        String msisdn = sessionMsisdn(request);

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

        String msisdn = sessionMsisdn(request);

        // Order of checks (C12): VAL001 first. A missing or empty amount is VAL001; an amount that is
        // present but not a whole number from 1 to 10000000 is BAL001 (values are never trimmed).
        if (amountPiasters == null || amountPiasters.isEmpty()
            || !InputValidation.isTxnRefNo(refNo)
            || !InputValidation.isTxnReason(reason)) {
            throw new BusinessException(ErrorCode.VAL001);
        }

        int parsedAmount = InputValidation.parseDebitAmount(amountPiasters)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAL001));

        DebitResult result = debitService.debit(msisdn, parsedAmount, refNo, reason);
        return ApiResponse.ok(Json.createObjectBuilder()
                .add(JsonKeys.TXN_ID, result.txnId())
                .add(JsonKeys.BALANCE_PIASTERS, result.balanceAfter())
                .build());
    }

    @POST
    @Path("/refund")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response refund(
            @FormParam("refNo") String refNo,
            @Context HttpServletRequest request) {

        String msisdn = sessionMsisdn(request);

        if (refNo == null || refNo.isEmpty()) {
            throw new BusinessException(ErrorCode.VAL001);
        }

        RefundResult result = refundService.refund(msisdn, refNo);
        return ApiResponse.ok(Json.createObjectBuilder()
                .add(JsonKeys.TXN_ID, result.txnId())
                .add(JsonKeys.AMOUNT_PIASTERS, result.amountPiasters())
                .add(JsonKeys.BALANCE_PIASTERS, result.balanceAfter())
                .build());
    }

    /** The subscriber behind the session (C11): the MSISDN is never taken from the request. */
    private static String sessionMsisdn(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new BusinessException(ErrorCode.SES001);
        }

        Object user = session.getAttribute(SessionKeys.USER);
        if (!(user instanceof String msisdn) || msisdn.isBlank()) {
            throw new BusinessException(ErrorCode.SES001);
        }
        return msisdn;
    }
}
