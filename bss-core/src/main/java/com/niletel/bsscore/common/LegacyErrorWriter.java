package com.niletel.bsscore.common;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public final class LegacyErrorWriter {

    private LegacyErrorWriter() {

    }

    public static void writeFail(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.httpStatus());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write((String) ApiResponse.fail(code).getEntity());
    }
}
