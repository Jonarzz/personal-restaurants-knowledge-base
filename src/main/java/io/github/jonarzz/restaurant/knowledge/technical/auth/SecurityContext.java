package io.github.jonarzz.restaurant.knowledge.technical.auth;

import java.util.*;

public class SecurityContext {

    private static final ThreadLocal<String> CURRENT_USER = new ThreadLocal<>();

    private SecurityContext() {

    }

    public static void setUserId(String userId) {
        CURRENT_USER.set(userId);
    }

    public static String getUserId() {
        return Optional.ofNullable(CURRENT_USER.get())
                       .orElseThrow(() -> new IllegalStateException("No user has been set in "
                                                                    + SecurityContext.class.getName()));
    }

}
