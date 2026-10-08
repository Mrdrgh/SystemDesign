package com.example.pastebin.Service.impl;

import com.example.pastebin.Entity.Paste;
import com.example.pastebin.Enum.AcessType;
import com.example.pastebin.Service.PasteAccessStrategy;
import com.example.pastebin.Service.AccessContext;

public class PublicAccessStrategyImpl implements PasteAccessStrategy {
    @Override
    public AcessType getSupportedAcessType() {
        return AcessType.PUBLIC;
    }

    @Override
    public void validateAccess(Paste paste, AccessContext accessContext) {
        return;
    }

}
