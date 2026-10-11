package com.example.pastebin.Service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.pastebin.Enum.AccessType;
import com.example.pastebin.Service.AccessContext;
import com.example.pastebin.Service.PasteAccessStrategy;
import com.example.pastebin.Exception.PasswordRequiredException;
import lombok.RequiredArgsConstructor;

import com.example.pastebin.Entity.Paste;;;

@Component
@RequiredArgsConstructor
public class PasswordAccessStrategy implements PasteAccessStrategy {
    private final PasswordEncoder passwordEncoder;
    @Override
    public AccessType getSupportedAcessType() {
        return AccessType.PASSWORD_PROTECTED;
    }

    @Override
    public void validateAccess(Paste paste, AccessContext context) {
        if (context.getProvidedPassword() == null || context.getProvidedPassword().isBlank()) {
            throw new PasswordRequiredException("This paste is password protected, password required");
        }

        if (!passwordEncoder.matches(context.getProvidedPassword(), paste.getPasswordHash())) {
            throw new PasswordRequiredException("Wrong password");
        }
    }
}
