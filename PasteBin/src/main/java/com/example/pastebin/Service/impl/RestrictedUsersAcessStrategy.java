package com.example.pastebin.Service.impl;

import org.springframework.stereotype.Component;

import com.example.pastebin.Enum.AccessType;
import com.example.pastebin.Service.AccessContext;
import com.example.pastebin.Service.PasteAccessStrategy;
import com.example.pastebin.Entity.*;
import com.example.pastebin.Exception.UnauthorizedException;

@Component
public class RestrictedUsersAcessStrategy implements PasteAccessStrategy {
    @Override
    public AccessType getSupportedAcessType() {
        return AccessType.ACCESS_LIST;
    }

    @Override
    public void validateAccess(Paste paste, AccessContext context) {
        User user = context.getCurrentUser();
        if (user == null) {
            throw new UnauthorizedException("Unauthorized, user is not set");
        }

        boolean isCreator = paste.getCreator() != null && paste.getCreator().getId().equals(user.getId());
        boolean isAuthorized = paste.getAuthorizedUsers().stream().anyMatch(u -> u.equals(user.getUsername()));

        if (!isCreator && !isAuthorized) {
            throw new UnauthorizedException("Unauthorized");
        }
    }
}
