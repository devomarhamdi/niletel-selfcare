package com.niletel.bsscore.health;

import com.niletel.bsscore.service.HealthService;
import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** GET /bss-core/health: 200 "UP" when the database answers, otherwise 503 "DOWN". No session, no envelope. */
@WebServlet(urlPatterns = "/health")
public class HealthServlet extends HttpServlet {

    @EJB
    private HealthService healthService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        boolean up;
        try {
            up = healthService.isDatabaseUp();
        } catch (RuntimeException e) {
            up = false;
        }
        response.setStatus(up ? HttpServletResponse.SC_OK : HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType("text/plain");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(up ? "UP" : "DOWN");
    }
}
