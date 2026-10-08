package com.example.pastebin.Service;

import java.nio.file.AccessDeniedException;

import com.example.pastebin.Entity.Paste;

import com.example.pastebin.Enum.AcessType;

public interface PasteAccessStrategy {
    AcessType getSupportedAcessType();

    void validateAccess(Paste paste, AccessContext context);
}
