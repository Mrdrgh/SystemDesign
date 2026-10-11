package com.example.pastebin.Exception;

/**
 * PasswordRequiredException
 */
public class PasswordRequiredException extends RuntimeException{
    public PasswordRequiredException(String message) {
        super(message);
    }
}
