package com.example.pastebin.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import javax.naming.NameNotFoundException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.pastebin.Exception.PasteNotAvailableException;
import com.example.pastebin.Entity.*;
import com.example.pastebin.Enum.PasteStatus;
import com.example.pastebin.repository.PasteRepository;
import com.example.pastebin.repository.UserRepository;
import com.example.pastebin.Exception.MaximumFileSizeException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import tools.jackson.databind.ObjectMapper;
import com.example.pastebin.Dto.*;

@Service
@RequiredArgsConstructor
public class PasteService {
    @Value("${paste.max-size-in-bytes:1000000}")
    private Long maxPasteSize;
    private final KeyGenService keyGenService;
    private final PasteRepository PasteRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3Service s3Service;
    private final PasteAccessService pasteAccessService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public PasteDto create(PasteDto paste) {
        if (paste.getSize() == null || paste.getSize() <= 0 || paste.getSize() > maxPasteSize) {
            throw new MaximumFileSizeException(
                    "invalid size, maximum paste size is of " + maxPasteSize / 1024 + " KB exceeded");
        }
        User creator = userRepository
                .findUserByUsername(SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(() -> new RuntimeException("user with the username provided does not exist"));

        // generate random unique url with md5
        String url = keyGenService.Md5generate();
        while (PasteRepository.findPasteByUrl(url).isPresent()) {
            url = keyGenService.Md5generate();
        }

        Paste new_paste = Paste.builder()
                .accessType(paste.getAccessType())
                .creator(creator)
                .passwordHash(paste.getPassword() == null ? null : passwordEncoder.encode(paste.getPassword()))
                .title(paste.getTitle())
                .expiration(paste.getExpiration())
                .url(url)
                .authorizedUsers(paste.getAuthorizedUsers())
                .build();
        PasteRepository.save(new_paste);

        String uploadUrl = s3Service.generateUploadUrl(url, paste.getSize());
        paste.setS3url(uploadUrl);

        return paste;
    }

    @SneakyThrows
    public PasteDto get(String url, String password, User currentUser) {

        // check cache
        String cahedJson = stringRedisTemplate.opsForValue().get("paste:" + url);
        Paste paste;
        if (cahedJson != null) {
            // cache hit
            PasteCacheDto dto = objectMapper.readValue(cahedJson, PasteCacheDto.class);

            paste = Paste.builder().id(dto.getId())
                    .title(dto.getTitle())
                    .url(dto.getUrl())
                    .accessType(dto.getAccessType())
                    .pasteStatus(dto.getPasteStatus())
                    .passwordHash(dto.getPasswordHash())
                    .expiration(dto.getExpiration())
                    .createdAt(dto.getCreatedAt())
                    .creator(dto.getCreatorId() != null
                            ? User.builder().id(dto.getCreatorId()).username(dto.getCreatorUsername()).build()
                            : null)
                    .authorizedUsers(dto.getAuthorizedUsers())
                    .build();
        } else {
            // cache miss, fetch db
            paste = PasteRepository.findPasteByUrl(url)
                    .orElseThrow(() -> new PasteNotAvailableException("paste with this url " + url + " is not found"));
            PasteCacheDto dtoToCache = PasteCacheDto.builder()
                    .id(paste.getId())
                    .title(paste.getTitle())
                    .url(paste.getUrl())
                    .accessType(paste.getAccessType())
                    .pasteStatus(paste.getPasteStatus())
                    .passwordHash(paste.getPasswordHash())
                    .expiration(paste.getExpiration())
                    .createdAt(paste.getCreatedAt())
                    .creatorId(paste.getCreator() != null ? paste.getCreator().getId() : null)
                    .creatorUsername(paste.getCreator() != null ? paste.getCreator().getUsername() : null)
                    .authorizedUsers(paste.getAuthorizedUsers())
                    .build();

            String jsonToCache = objectMapper.writeValueAsString(dtoToCache);
            if (dtoToCache.getExpiration() != null) {
                stringRedisTemplate.opsForValue().set("paste:" + url, jsonToCache, dtoToCache.getExpiration());
            } else {
                stringRedisTemplate.opsForValue().set("paste:" + url, jsonToCache, java.time.Duration.ofDays(1));
            }
        }

        // check if the paste is not expired
        if (paste.getExpiration() != null
                && paste.getCreatedAt().plus(paste.getExpiration()).isBefore(LocalDateTime.now())) {
            paste.setPasteStatus(PasteStatus.EXPIRED);
            PasteRepository.save(paste);
            stringRedisTemplate.delete("paste:" + url);
            throw new PasteNotAvailableException(
                    "the paste with url : " + url + " has expired or has been deleted and is no longer accessible");
        }

        if (paste.getPasteStatus() == PasteStatus.UPLOAD_PENDING) {
            throw new PasteNotAvailableException(
                    "the paste with url : " + url + " is still pending to be uploaded and is not yet accessible");
        }

        AccessContext ac = new AccessContext(currentUser, password);

        // check access to this paste
        pasteAccessService.checkAccess(paste, ac);

        // increment reads in Redis
        stringRedisTemplate.opsForValue().increment("reads:" + url);

        // generate presigned get url
        String s3url = s3Service.generateDownloadUrl(url);

        return PasteDto.builder().title(paste.getTitle())
                .createdAt(paste.getCreatedAt())
                .creator(paste.getCreator() != null ? paste.getCreator().getUsername() : null)
                .s3url(s3url)
                .expiration(paste.getExpiration())
                .title(paste.getTitle())
                .url(paste.getUrl())
                .id(paste.getId())
                .build();
    }
}
