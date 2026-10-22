package com.spendly.spendly.service;

import com.spendly.spendly.domain.AppUser;
import com.spendly.spendly.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/** The signed-in user. Every query in Spendly is scoped to this user. */
@Component
@RequiredArgsConstructor
public class CurrentUser {

    private static final String KEY = CurrentUser.class.getName();

    private final AppUserRepository userRepository;

    public AppUser get() {
        AppUser user = find();
        if (user == null) {
            throw new IllegalStateException("No signed-in user");
        }
        return user;
    }

    /** The signed-in user, loaded once per request; null for visitors. */
    public AppUser find() {
        String name = SecurityUtils.currentUsername();
        if (name == null) {
            return null;
        }
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs != null && attrs.getAttribute(KEY, RequestAttributes.SCOPE_REQUEST) instanceof AppUser cached
                && cached.getUsername().equalsIgnoreCase(name)) {
            return cached;
        }
        AppUser user = userRepository.findByUsernameIgnoreCase(name).orElse(null);
        if (attrs != null && user != null) {
            attrs.setAttribute(KEY, user, RequestAttributes.SCOPE_REQUEST);
        }
        return user;
    }
}
