package com.example.pastebin.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class KeyGenService {

    @Value("${key-gen.length:16}")
    public int length;

    public String UUIDgenerate() {
        return UUID.randomUUID().toString().substring(length);
    }

    public String Md5generate() {
        try {
            byte[] seed = new byte[length];
            new SecureRandom().nextBytes(seed);

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(seed);

            return HexFormat.of().formatHex(digest);
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Exception during the keygen process: " + e.getMessage());
        }
    }
}