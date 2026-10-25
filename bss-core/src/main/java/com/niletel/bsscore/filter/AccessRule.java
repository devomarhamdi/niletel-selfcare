package com.niletel.bsscore.filter;

import java.util.Set;
import java.util.regex.Pattern;

public final class AccessRule {

    private final Pattern pathPattern;
    private final Set<String> allowedRules;

    public AccessRule(String pathRegex, Set<String> allowedRules) {
        this.pathPattern = Pattern.compile(pathRegex);
        this.allowedRules = allowedRules;
    }

    public boolean matches(String path){
        return pathPattern.matcher(path).matches();
    }
    public boolean allows(String role){
        return allowedRules.contains(role);
    }
}
