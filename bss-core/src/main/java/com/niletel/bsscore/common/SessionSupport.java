package com.niletel.bsscore.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public final class SessionSupport {

    private SessionSupport() {
    }

    public static void startSession(HttpServletRequest request, String user, String role) {
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession newSession = request.getSession(true);
        newSession.setAttribute(SessionKeys.USER, user);
        newSession.setAttribute(SessionKeys.ROLE, role);
    }

    public static String currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return (String) session.getAttribute(SessionKeys.USER);
    }
}
