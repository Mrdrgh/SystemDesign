package com.example.tinyurl.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.Map;

@ControllerAdvice
@ResponseBody
public class ControllerExceptionsHandler {

    @ExceptionHandler({ UrlNotFoundException.class, Exception.class })
    public ResponseEntity<Map<String, String>> handle(Exception e) {
        return new ResponseEntity<>(Map.of("message", e.getMessage()), HttpStatus.NOT_FOUND);
    }
}
