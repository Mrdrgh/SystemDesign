package com.example.pastebin.Dto;

import jakarta.validation.constraints.Email;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
@Builder
public class RegisterRequestDto {
    private String username;
    @Email
    private String email;

    private String password;
}
