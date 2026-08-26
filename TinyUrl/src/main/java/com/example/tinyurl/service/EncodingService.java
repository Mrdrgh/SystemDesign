package com.example.tinyurl.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class EncodingService {

    public static int SHORT_URL_CHAR_SIZE = 7; // +1

    public String sha256Encoder(String longString) {
        /* TODO - implement sha256 */
        return "null";
    }

    public String UuidEncoder() {
        return UUID.randomUUID().toString().substring(0, SHORT_URL_CHAR_SIZE + 1);
    }
}
