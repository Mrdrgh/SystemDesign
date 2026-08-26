package com.example.tinyurl.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.tinyurl.Repository.UrlRepository;
import java.util.Set;
import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class VisitsFlushScheduler {
    private final StringRedisTemplate stringRedisTemplate;
    private final UrlRepository urlRepository;

    @Scheduled(fixedRate = 60000) // 1 min
    public void flushVisits() {
        Set<String> keys = stringRedisTemplate.keys("visits:*");
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for (String key : keys) {
            String shortUrl = key.replace("visits:", "");
            String pendingVistsStr = stringRedisTemplate.opsForValue().getAndDelete(key);

            if (pendingVistsStr != null) {
                Long pendingVisits = Long.parseLong(pendingVistsStr);
                urlRepository.findUrlByShortUrl(shortUrl).ifPresent(url -> {
                    url.setVisits(url.getVisits() + pendingVisits);
                    urlRepository.save(url);
                });
            }
        }
    }
}
