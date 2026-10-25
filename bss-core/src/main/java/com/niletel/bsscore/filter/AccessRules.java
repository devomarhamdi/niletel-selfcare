package com.niletel.bsscore.filter;

import jakarta.persistence.Access;

import java.util.List;
import java.util.Set;

public final class AccessRules {

    public static final String SUB = "SUB";
    public static final String OPR = "OPR";
    public static final String SYS = "SYS";

    private static final Set<String> ANY_ROLE = Set.of(SUB, OPR, SYS);

    private static final Set<String> OPEN_PATHS =
            Set.of("/auth/login", "/auth/operator-login");

    private static final String ME_PATHS =
            "/subscriber/me(/(debit|recharge|refund|transactions))?";

    public static final List<AccessRule> RULES = List.of(
            new AccessRule("/auth/logout", ANY_ROLE),
            new AccessRule("/tariffs", ANY_ROLE),
            new AccessRule("ME_PATHS", Set.of(SUB)),
            new AccessRule("/subscribers/[^/]+", Set.of(OPR,SYS))
    );

    private AccessRules() {}

    public static boolean isOpen(String path){
        return OPEN_PATHS.contains(path);
    }

    public static boolean isAllowed(String path, String role){
        for(AccessRule rule: RULES){
            if(rule.matches(path)){
                return rule.allows(role);
            }
        }
        return false;
    }
}
