package com.example.pastebin.Service;

import java.time.LocalDateTime;

import javax.naming.NameNotFoundException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.pastebin.Exception.PasteNotAvailableException;
import com.example.pastebin.Dto.PasteDto;
import com.example.pastebin.Entity.*;
import com.example.pastebin.Enum.PasteStatus;
import com.example.pastebin.repository.PasteRepository;
import com.example.pastebin.repository.UserRepository;
import com.example.pastebin.Exception.MaximumFileSizeException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasteService {
    @Value("${paste.max-size-in-bytes:1000000}")
    private Long maxPasteSize;
    private final KeyGenService keyGenService;
    private final PasteRepository PasteRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3Service s3Service;
    private final PasteAccessService pasteAccessService;

    public PasteDto create(PasteDto paste) {
        if (paste.getSize() == null || paste.getSize() <= 0 || paste.getSize() > maxPasteSize) {
            throw new MaximumFileSizeException("invalid size, maximum paste size is of " + maxPasteSize / 1024 + " KB exceeded");
        }
        User creator = userRepository.findUserByUsername(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(() -> new RuntimeException("user with the username provided does not exist"));

        // generate random unique url with md5
        String url = keyGenService.Md5generate();
        while (PasteRepository.findPasteByUrl(url).isPresent()) {
            url = keyGenService.Md5generate();
        }



        Paste new_paste = Paste.builder()
                            .accessType(paste.getAccessType())
                            .creator(creator)
                            .passwordHash(paste.getPassword() == null ? null : passwordEncoder.encode(paste.getPassword()))
                            .title(paste.getTitle())
                            .expiration(paste.getExpiration())
                            .url(url)
                            .authorizedUsers(paste.getAuthorizedUsers())
                            .build();
        PasteRepository.save(new_paste);

        String uploadUrl = s3Service.generateUploadUrl(url, paste.getSize());
        paste.setS3url(uploadUrl);

        return paste;
    }
    

    public PasteDto get(String url, String password, User currentUser) {
        Paste paste = PasteRepository.findPasteByUrl(url).orElseThrow(() -> new PasteNotAvailableException("paste url : " + url + " is not found"));

        // check if the paste is not expired
        if (paste.getExpiration() != null && paste.getCreatedAt().plus(paste.getExpiration()).isBefore(LocalDateTime.now())) {
            throw new PasteNotAvailableException("the paste with url : " + url + " has expired and is no longer accessible");
        }

        if (paste.getPasteStatus() == PasteStatus.UPLOAD_PENDING) {
            throw new PasteNotAvailableException("the paste with url : " + url + " is still pending to be uploaded and is not yet accessible");
        }

        AccessContext ac = new AccessContext(currentUser, password);
        
        // check access to this paste
        pasteAccessService.checkAccess(paste, ac);

        //generate presigned get url
        String s3url = s3Service.generateDownloadUrl(url);

        // increment the reads
        paste.setReads(paste.getReads() + 1);
        PasteRepository.save(paste);
        PasteDto pasteDto = PasteDto.builder().s3url(s3url).title(paste.getTitle()).url(paste.getUrl()).build();
        return pasteDto;
    }
}
