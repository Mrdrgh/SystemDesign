package com.example.pastebin.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.core.AuthenticationException;
import com.example.pastebin.Dto.ExceptionDto;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class ControllerExceptionsHandler {

    @ExceptionHandler(MaximumFileSizeException.class)
    public ResponseEntity<ExceptionDto> handleMaximumFileSizeException(MaximumFileSizeException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ExceptionDto.builder()
                        .status(HttpStatus.PAYLOAD_TOO_LARGE.value())
                        .message(e.getMessage())
                        .build());
    }

    @ExceptionHandler({ UnauthorizedException.class, AuthenticationException.class })
    public ResponseEntity<ExceptionDto> handleUnauthorizedException(Exception e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ExceptionDto.builder()
                        .status(HttpStatus.FORBIDDEN.value())
                        .message(e.getMessage())
                        .build());
    }

    @ExceptionHandler(PasswordRequiredException.class)
    public ResponseEntity<ExceptionDto> handlePasswordRequiredException(PasswordRequiredException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ExceptionDto.builder()
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .message(e.getMessage())
                        .build());
    }

    @ExceptionHandler(PasteNotAvailableException.class)
    public ResponseEntity<ExceptionDto> handlePasteNotAvailableException(PasteNotAvailableException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ExceptionDto.builder()
                        .status(HttpStatus.NOT_FOUND.value())
                        .message(e.getMessage())
                        .build());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ExceptionDto> handleRateLimitExceptin(RateLimitExceededException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ExceptionDto.builder().status(HttpStatus.TOO_MANY_REQUESTS.value())
                        .message(e.getMessage()).build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionDto> handleGenericException(Exception e) {
        log.error(e.getMessage());
        log.error("stack trace : ");
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ExceptionDto.builder()
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .message("An unexpected error occurred")
                        .build());
    }

}
