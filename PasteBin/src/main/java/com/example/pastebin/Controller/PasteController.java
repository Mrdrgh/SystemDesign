package com.example.pastebin.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.pastebin.Dto.PasteCreationRequestDto;
import com.example.pastebin.Dto.PasteCreationResponseDto;
import com.example.pastebin.Dto.PasteDto;
import com.example.pastebin.Entity.User;
import com.example.pastebin.Service.*;

import jakarta.websocket.server.PathParam;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pastes")
public class PasteController {
    private final PasteService pasteService;

    @PostMapping("/create")
    public ResponseEntity<PasteCreationResponseDto> createPaste(@RequestBody PasteCreationRequestDto pasteDto) {
        PasteCreationResponseDto signedPaste = pasteService.create(pasteDto);
        return ResponseEntity.ok(signedPaste);
    }

    @GetMapping("/{url}")
    public ResponseEntity<PasteDto> getPaste(@PathVariable String url, @RequestParam(required = false) String password,
            @AuthenticationPrincipal User currentUser) {
        PasteDto pasteDto = pasteService.get(url, password, currentUser);
        return ResponseEntity.ok(pasteDto);
    }

}
