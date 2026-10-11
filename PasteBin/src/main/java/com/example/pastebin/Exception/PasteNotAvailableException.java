package com.example.pastebin.Exception;

public class PasteNotAvailableException extends RuntimeException {
    public PasteNotAvailableException(String message) {
        super(message);
    }
}