package com.example.tinyurl.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Url {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true)
    String shortUrl;

    @Column(nullable = false, unique = true)
    String longUrl;

    @ManyToOne
    @JoinColumn(name = "creator")
    User creator;

    @CreationTimestamp
    LocalDateTime createdAt;

    @Builder.Default
    Long visits = 0L;
}
