package com.example.tinyurl.service;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

import java.time.Duration;
import java.util.*;

@Service
public class RateLimiterService {
    // each client with a bucket
    private final Map<String, Bucket> cach = new ConcurrentHashMap<>();

    @Value("${app.ratelimit.capacity:5}")
    private int capacity;

    @Value("${app.ratelimit.refill-in-seconds:10}")
    private int refill;

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.builder().capacity(capacity).refillGreedy(5, Duration.ofSeconds(refill)).build();
        return Bucket.builder().addLimit(limit).build();
    }

    public Bucket resolveBucket(String key) {
        return cach.computeIfAbsent(key, k -> this.createNewBucket());
    }

    public boolean tryConsume(String key) {
        Bucket bucket = resolveBucket(key);
        return bucket.tryConsume(1);
    }
}
