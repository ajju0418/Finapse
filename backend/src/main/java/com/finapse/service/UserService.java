package com.finapse.service;

import com.finapse.entity.User;
import com.finapse.exception.UnauthorizedException;
import com.finapse.repository.UserRepository;
import com.finapse.security.FinapseUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Resolves the caller behind the current request.
 *
 * <p>Every user-scoped query in the application funnels through here, so data can
 * never be read or written on behalf of anyone but the authenticated principal.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /** The authenticated user's id, taken straight from the verified access token. */
    public UUID getCurrentUserId() {
        return currentPrincipal().getId();
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        UUID id = getCurrentUserId();
        return userRepository.findById(id)
                .orElseThrow(() -> new UnauthorizedException("This account no longer exists."));
    }

    private FinapseUserDetails currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof FinapseUserDetails principal)) {
            throw new UnauthorizedException("Authentication is required.");
        }
        return principal;
    }
}
