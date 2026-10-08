package com.example.pastebin.Service.impl;

import org.springframework.stereotype.Component;

import com.example.pastebin.Enum.AcessType;
import com.example.pastebin.Service.AccessContext;
import com.example.pastebin.Service.PasteAccessStrategy;
import com.example.pastebin.Entity.Paste;;;

@Component
public class PasswordAccessStrategy implements PasteAccessStrategy {

    @Override
    public AcessType getSupportedAcessType() {
        return AcessType.PASSWORD_PROTECTED;
    }

    @Override
    public void validateAccess(Paste paste, AccessContext context) {
        if (context.getProvidedPassword() == null || context.getProvidedPassword().isBlank()) {
            throw new PasswordRequiredException("This paste is password protected, password required");
        }

        if (!passwordEncoder.matches(context.getProvidedPassword(), paste.getPasswordHash())) {
            throw new InvalidPasswordException("Wrong password");
        }
    }
}
