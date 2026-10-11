package com.example.pastebin.Entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Builder.Default;
import java.util.*;

import org.hibernate.annotations.CreationTimestamp;

import com.example.pastebin.Enum.AccessType;
import com.example.pastebin.Enum.PasteStatus;

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
    private AccessType accessType = AccessType.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Default
    private PasteStatus pasteStatus = PasteStatus.UPLOAD_PENDING;

    private String passwordHash;

    @ElementCollection
    @JoinTable(name = "paste_authorized_users", joinColumns = @JoinColumn(name = "paste_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    @Default
    private Set<String> authorizedUsers = new HashSet<>();
}
