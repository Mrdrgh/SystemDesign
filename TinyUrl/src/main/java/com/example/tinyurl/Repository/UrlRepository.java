package com.example.tinyurl.Repository;

import java.util.Optional;
import com.example.tinyurl.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {
    Optional<Url> findUrlByShortUrl(String shortUrl);
}
