package com.example.pastebin.Service;

import com.example.pastebin.Entity.User;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccessContext {
    private final User currentUser;
    private final String providedPassword;
}
