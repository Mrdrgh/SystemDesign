package com.example.pastebin.Dto;

import java.time.Duration;

import com.example.pastebin.Enum.AccessType;
import java.util.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter 
@Setter 
@Builder 
@AllArgsConstructor
@NoArgsConstructor 
public class PasteDto {
    private String title;
    private Duration expiration;
    private AccessType accessType;
    private String password;
    private Set<String> authorizedUsers;
    private String url;
    private String s3url;
    private Long size;
}
