package com.example.pastebin.Dto;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.example.pastebin.Enum.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PasteCacheDto {
    private Long id;
    private String title;
    private String url;
    private Duration expiration;
    private LocalDateTime createdAt;
    private AccessType accessType;
    private PasteStatus pasteStatus;
    private String passwordHash;
    private String creatorId;
    private String creatorUsername;
    private Set<String> authorizedUsers;
}
