package com.example.pastebin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.pastebin.Entity.*;

@Repository
public interface PasteRepository extends JpaRepository<Paste, Long> {
}
