package com.example.pastebin.Exception;


public class MaximumFileSizeException extends RuntimeException {
    public MaximumFileSizeException(String message) {
        super(message);
    }
}
