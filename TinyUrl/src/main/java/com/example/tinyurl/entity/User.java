package com.example.tinyurl.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import lombok.Builder.Default;

import java.util.*;
import com.example.tinyurl.entity.Url;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Builder
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = true, unique = true)
    @Email
    String email;

    @OneToMany(mappedBy = "creator")
    @Default
    @JsonIgnore
    List<Url> urls = new ArrayList<>();
}
