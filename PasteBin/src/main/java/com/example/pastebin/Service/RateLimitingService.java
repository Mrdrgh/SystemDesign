package com.example.pastebin.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.time.Duration;
import java.util.Map;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

import org.springframework.stereotype.Service;

@Service
public class RateLimitingService {
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    private int capacity = 5;
    private int refill_in_seconds = 30;

    private Bucket crateNewBucket() {
        Bandwidth bw = Bandwidth.builder().capacity(capacity)
                .refillGreedy(capacity, Duration.ofSeconds(refill_in_seconds)).build();
        return Bucket.builder().addLimit(bw).build();
    }

    public Bucket resolveBucket(String identifier) {
        return cache.computeIfAbsent(identifier, k -> this.crateNewBucket());
    }

    public ConsumptionProbe consumeToken(String identifier) {
        Bucket bucket = resolveBucket(identifier);
        return bucket.tryConsumeAndReturnRemaining(1);
    }
}
