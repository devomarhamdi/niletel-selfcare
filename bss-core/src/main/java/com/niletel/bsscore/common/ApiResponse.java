package com.niletel.bsscore.common;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonStructure;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * The only place that builds the response envelope.
 * Success: {"RESULT":"OK","DATA":...}. Failure: {"RESULT":"FAIL","ERR_CODE":"XXX000","ERR_MSG":"..."}.
 */
public final class ApiResponse {

    private static final String JSON_UTF8 = MediaType.APPLICATION_JSON + ";charset=UTF-8";

    private ApiResponse() {
    }

    public static Response ok(JsonStructure data) {
        JsonObject body = Json.createObjectBuilder()
                .add(JsonKeys.RESULT, JsonKeys.RESULT_OK)
                .add(JsonKeys.DATA, data)
                .build();
        return build(200, body);
    }

    public static Response ok() {
        return ok(Json.createObjectBuilder().build());
    }

    public static Response fail(ErrorCode errorCode) {
        JsonObjectBuilder body = Json.createObjectBuilder()
                .add(JsonKeys.RESULT, JsonKeys.RESULT_FAIL)
                .add(JsonKeys.ERR_CODE, errorCode.code())
                .add(JsonKeys.ERR_MSG, errorCode.defaultMessage());
        return build(errorCode.httpStatus(), body.build());
    }

    private static Response build(int status, JsonObject body) {
        return Response.status(status).type(JSON_UTF8).entity(body.toString()).build();
    }
}
