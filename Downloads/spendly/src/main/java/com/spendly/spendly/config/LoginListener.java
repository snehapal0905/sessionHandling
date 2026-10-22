package com.spendly.spendly.config;

import com.spendly.spendly.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginListener {

    private final UserService userService;

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        userService.recordLogin(event.getAuthentication().getName());
    }
}
