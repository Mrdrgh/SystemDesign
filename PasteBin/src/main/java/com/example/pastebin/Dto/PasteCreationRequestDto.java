package com.example.pastebin.Dto;

import java.util.Set;

import com.example.pastebin.Enum.AccessType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasteCreationRequestDto {
    private String title;
    private Long expirationInMinutes;
    private String accessType;
    private String password;
    private Set<String> authorizedUsers;
    private Long sizeInBytes;
}
