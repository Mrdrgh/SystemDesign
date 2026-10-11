package com.example.pastebin.Service.impl;

import com.example.pastebin.Service.AccessContext;
import com.example.pastebin.Service.PasteAccessStrategy;

import org.springframework.stereotype.Component;
import com.example.pastebin.Exception.*;

import com.example.pastebin.Entity.Paste;
import com.example.pastebin.Enum.*;

@Component
public class PrivateAccessStrategy implements PasteAccessStrategy{
    
    @Override
    public AccessType getSupportedAcessType() {
        return AccessType.PRIVATE;
    }

    @Override
    public void validateAccess(Paste paste, AccessContext context) {
        if (context.getCurrentUser() == null || paste.getCreator() == null || !context.getCurrentUser().getId().equals(paste.getCreator().getId()))
            throw new UnauthorizedException("this paste is private, and you are not authorized to access it");
        }
}
