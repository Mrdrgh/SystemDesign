package com.example.pastebin.Service.impl;

import org.springframework.stereotype.Component;

import com.example.pastebin.Entity.Paste;
import com.example.pastebin.Enum.AccessType;
import com.example.pastebin.Service.PasteAccessStrategy;
import com.example.pastebin.Service.AccessContext;

@Component
public class PublicAccessStrategyImpl implements PasteAccessStrategy {
    @Override
    public AccessType getSupportedAcessType() {
        return AccessType.PUBLIC;
    }

    @Override
    public void validateAccess(Paste paste, AccessContext accessContext) {
        return;
    }

}
