package com.chatapp.security;

import org.springframework.security.core.Authentication;

// Piccolo aiuto per evitare di ripetere in ogni controller il cast
// "Authentication -> CustomUserPrincipal".
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long currentUserId(Authentication authentication) {
        return ((CustomUserPrincipal) authentication.getPrincipal()).getId();
    }

    public static CustomUserPrincipal currentPrincipal(Authentication authentication) {
        return (CustomUserPrincipal) authentication.getPrincipal();
    }
}
