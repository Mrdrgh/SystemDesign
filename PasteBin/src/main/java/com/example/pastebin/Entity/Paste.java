package com.example.pastebin.Entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Builder.Default;
import java.util.*;

import org.hibernate.annotations.CreationTimestamp;

import com.example.pastebin.Enum.AcessType;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Paste {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(nullable = false, unique = true, length = 16)
    private String url;
    private String s3Url;
    private Duration expiration;

    @Builder.Default
    private Long reads = 0L;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User creator;

    @Enumerated(EnumType.STRING)
    @Default
    private AcessType accessType = AcessType.PUBLIC;

    private String passwordHash;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "paste_authorized_users", joinColumns = @JoinColumn(name = "paste_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    @Default
    private Set<User> authorizedUsers = new HashSet<>();
}
