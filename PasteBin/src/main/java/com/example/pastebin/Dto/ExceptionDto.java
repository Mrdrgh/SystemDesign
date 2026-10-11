package com.example.pastebin.Dto;

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
public class ExceptionDto {
    private int status;
    private String message;
}
