package com.cet6.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {}

    public static LoginUser getLoginUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    public static Long getUserId() {
        LoginUser user = getLoginUser();
        return user != null ? user.getUserId() : null;
    }

    public static String getRole() {
        LoginUser user = getLoginUser();
        return user != null ? user.getRole() : null;
    }

    public static boolean isAdmin() {
        return "admin".equals(getRole());
    }

    public static boolean isStudent() {
        return "student".equals(getRole());
    }
}
