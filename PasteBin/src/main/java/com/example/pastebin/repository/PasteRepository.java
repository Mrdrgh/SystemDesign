package com.example.pastebin.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.pastebin.Entity.*;
import com.example.pastebin.Enum.PasteStatus;

@Repository
public interface PasteRepository extends JpaRepository<Paste, Long> {
    Optional<Paste> findPasteByUrl(String url);

    @Query("SELECT p.url from Paste p where p.pasteStatus = 'EXPIRED'")
    List<String> findPastesByPasteStatusIsExpired();
}