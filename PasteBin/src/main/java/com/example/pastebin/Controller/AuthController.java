package com.example.pastebin.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.pastebin.Dto.AuthResponseDto;
import com.example.pastebin.Dto.LoginRequestDto;
import com.example.pastebin.Dto.RegisterRequestDto;
import com.example.pastebin.Service.AuthenticationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@RequestBody RegisterRequestDto req) {
        return ResponseEntity.ok(authenticationService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto req) {
        return ResponseEntity.ok(authenticationService.login(req));
    }
}
