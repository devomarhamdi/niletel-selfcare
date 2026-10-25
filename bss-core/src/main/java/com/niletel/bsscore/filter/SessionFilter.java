package com.niletel.bsscore.filter;

import com.niletel.bsscore.common.ErrorCode;
import com.niletel.bsscore.common.LegacyErrorWriter;
import com.niletel.bsscore.common.SessionKeys;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

public class SessionFilter implements Filter {

    private static final String API_PREFIX = "/api";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
        throws IOException, ServletException{
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String path = matchedPath(request);

        if(AccessRules.isOpen(path)){
            chain.doFilter(request, response);
            return;
        }

        String role = roleOf(request.getSession(false));
        if (role == null){
            LegacyErrorWriter.writeFail(response, ErrorCode.SES001);
            return;
        }
        if(!AccessRules.isAllowed(path, role)){
            LegacyErrorWriter.writeFail(response, ErrorCode.SEC001);
            return;
        }
        chain.doFilter(request, response);
    }

    private String matchedPath(HttpServletRequest request) {
        String pathInfo = request.getPathInfo() == null ? "" : request.getPathInfo();
        String fullPath = request.getServletPath() + pathInfo;
        return fullPath.startsWith(API_PREFIX) ? fullPath.substring(API_PREFIX.length()) : fullPath;
    }

    private String roleOf(HttpSession session) {
        return session == null ? null : (String) session.getAttribute(SessionKeys.ROLE);
    }

}
