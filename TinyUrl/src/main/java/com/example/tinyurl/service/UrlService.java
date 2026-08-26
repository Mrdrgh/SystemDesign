package com.example.tinyurl.service;

import org.slf4j.Logger;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.example.tinyurl.entity.Url;
import com.example.tinyurl.entity.User;
import com.example.tinyurl.exceptions.UrlNotFoundException;
import com.example.tinyurl.Repository.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.*;

@Service
@AllArgsConstructor
@Slf4j
public class UrlService {
    private final StringRedisTemplate stringRedisTemplate;
    private final UrlRepository urlRepository;
    private final UserRepository userRepository;
    private final EncodingService encodingService;

    public String encode(String longUrl, String email) {
        // encode long url using encoding service
        String shortUrl = encodingService.UuidEncoder();

        // make sure its shorturl doesnt exist in db
        while (urlRepository.findUrlByShortUrl(shortUrl).isPresent())
            shortUrl = encodingService.UuidEncoder();
        // save url class in db
        Url url = null;
        if (email != null) {
            // fetch existing user or create a new one
            User user = userRepository.findUserByEmail(email).orElseGet(
                    () -> userRepository.save(User.builder().email(email).build()));
            // check before encoding if the user already has shortened the long url
            List<Url> userUrls = userRepository.findUserByEmail(email)
                    .map(User::getUrls)
                    .orElse(Collections.emptyList());
            for (Url u : userUrls) {
                if (u.getLongUrl().equals(longUrl))
                    return u.getShortUrl();
            }
            url = Url.builder().longUrl(longUrl).shortUrl(shortUrl).creator(user).build();
            userRepository.save(user);
        } else
            url = Url.builder().longUrl(longUrl).shortUrl(shortUrl).build();
        urlRepository.save(url);
        return url.getShortUrl();
    }

    public String getUrl(String shortUrl) {
        // look for shorturl in cache
        String cacheKey = "url:" + shortUrl;
        String visitsKey = "visits:" + shortUrl;

        String cachedLongUrl = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedLongUrl != null) {
            // cache hit
            stringRedisTemplate.opsForValue().increment(visitsKey);
            log.info("retrieved long url : " + cachedLongUrl + "from cache");
            return cachedLongUrl;
        }

        // cache miss

        // look for it in db : if found return it and increase the visit count, else
        // return null
        Optional<Url> urlOptional = urlRepository.findUrlByShortUrl(shortUrl);
        if (urlOptional.isPresent()) {
            Url url = urlOptional.get();
            // put url in cache
            stringRedisTemplate.opsForValue().set(cacheKey, url.getLongUrl(), Duration.ofDays(1));
            stringRedisTemplate.opsForValue().increment(visitsKey);
            return url.getLongUrl();
        } else {
            return null;
        }
    }

    public Url findUrl(String mail, String ShortUrl) throws UrlNotFoundException {
        Optional<User> user = userRepository.findUserByEmail(mail);
        if (user.isPresent()) {
            Url url = urlRepository.findUrlByShortUrl(ShortUrl)
                    .orElseThrow(() -> new UrlNotFoundException("no url found for " + ShortUrl));
            String pendingVisitsStr = stringRedisTemplate.opsForValue().getAndDelete("visits:" + ShortUrl);
            if (pendingVisitsStr != null) {
                url.setVisits(url.getVisits() + Long.parseLong(pendingVisitsStr));
                urlRepository.save(url);
            }
            return url;
        } else {
            return null;
        }
    }
}
