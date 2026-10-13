package com.example.pastebin.Service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationManager;
import com.example.pastebin.Entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.pastebin.Dto.AuthResponseDto;
import com.example.pastebin.Dto.LoginRequestDto;
import com.example.pastebin.Dto.RegisterRequestDto;
import com.example.pastebin.Enum.UserRole;
import com.example.pastebin.Exception.UnauthorizedException;
import com.example.pastebin.repository.UserRepository;

import jakarta.websocket.server.ServerEndpoint;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponseDto register(RegisterRequestDto requestDto) {
        if (userRepository.findUserByUsername(requestDto.getUsername()).isPresent()
                || userRepository.findUserByEmail(requestDto.getEmail()).isPresent())
            throw new UnauthorizedException("the email or username already exists");
        User user = User.builder().username(requestDto.getUsername())
                .email(requestDto.getEmail())
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .role(UserRole.USER)
                .build();

        userRepository.save(user);

        String jwtToken = jwtService.generateToken(user);

        return new AuthResponseDto(jwtToken);
    }

    public AuthResponseDto login(LoginRequestDto login) {
        authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(login.getUsername(), login.getPassword()));

        // if we reach this line the user is valid and no exception was thrown
        // (AuthenticationException) by the auth manager
        User user = userRepository.findUserByUsername(login.getUsername())
                .orElseThrow(() -> new RuntimeException("user was not found"));

        // issue a jwt badge for the authenticated user
        String jwtToken = jwtService.generateToken(user);
        return new AuthResponseDto(jwtToken);
    }
}
