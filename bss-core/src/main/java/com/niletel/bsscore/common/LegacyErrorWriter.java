package com.niletel.bsscore.common;

import jakarta.json.Json;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public final class LegacyErrorWriter {

    private LegacyErrorWriter() {

    }

    public static void writeFail(HttpServletResponse response, int httpStatus, String errCode, String errMsg) throws IOException {
        String body = Json.createObjectBuilder()
                .add("RESULT", "FAIL")
                .add("ERR_CODE", errCode)
                .add("ERR_MSG", errMsg)
                .build().toString();
        response.setStatus(httpStatus);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(body);
    }
}
