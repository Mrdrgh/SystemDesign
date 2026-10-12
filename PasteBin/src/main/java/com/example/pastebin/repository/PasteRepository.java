package com.example.pastebin.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.pastebin.Entity.*;
import com.example.pastebin.Enum.PasteStatus;

@Repository
public interface PasteRepository extends JpaRepository<Paste, Long> {
    Optional<Paste> findPasteByUrl(String url);

    @Query("SELECT p.url from Paste p where p.pasteStatus = 'EXPIRED'")
    List<String> findPastesByPasteStatusIsExpired();

    @Modifying
    @Query("""
                UPDATE Paste p
                SET p.pasteStatus = :expiredStatus
                WHERE p.pasteStatus = :pendingStatus
                AND p.createdAt < :cutoff
            """)
    int updateObsoletePastesStatus(
            @Param("expiredStatus") PasteStatus expiredStatus,
            @Param("pendingStatus") PasteStatus pendingStatus,
            @Param("cutoff") LocalDateTime cutoff);

}