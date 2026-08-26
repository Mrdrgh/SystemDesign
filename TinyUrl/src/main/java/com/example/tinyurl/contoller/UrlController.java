package com.example.tinyurl.contoller;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.tinyurl.entity.Url;
import com.example.tinyurl.exceptions.UrlNotFoundException;

import java.net.URI;
import java.util.Map;
import com.example.tinyurl.service.UrlService;

import lombok.AllArgsConstructor;

@Controller
@RequestMapping("/")
@AllArgsConstructor
public class UrlController {
    private final UrlService urlService;

    @PostMapping
    @ResponseBody
    public ResponseEntity<Map<String, String>> createUrl(@RequestBody Map<String, String> request) {
        String longUrl = request.get("long-url");
        String email = request.get("creator");
        if (longUrl == null)
            return new ResponseEntity<>(
                    Map.of("error", "long-url field is not found in the request", "status", "404"),
                    HttpStatus.BAD_REQUEST);

        String shortUrl = urlService.encode(longUrl, email);

        return new ResponseEntity<>(Map.of("short-url", shortUrl), HttpStatus.OK);
    }

    @GetMapping("/{short-url}")
    public ResponseEntity<Void> getLongUrl(@PathVariable(name = "short-url") String shortUrl) throws Exception {
        String longUrl = urlService.getUrl(shortUrl);
        if (longUrl != null)
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(longUrl)).build();
        else
            throw new UrlNotFoundException("no url found for " + shortUrl);
    }

    @GetMapping("/{short-url}/stats")
    @ResponseBody
    public ResponseEntity<Url> getUrlStats(@PathVariable(name = "short-url") String shortUrl,
            @RequestBody Map<String, String> request) throws Exception {
        Url url = urlService.findUrl(request.get("email"), shortUrl);
        if (url == null) {
            throw new Exception("no email found for : " + request.get("email"));
        }
        return new ResponseEntity<>(url, HttpStatus.OK);
    }
}
