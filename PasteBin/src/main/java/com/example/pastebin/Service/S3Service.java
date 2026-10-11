package com.example.pastebin.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.pastebin.repository.PasteRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Presigner s3Presigner;
    private final PasteRepository pasteRepository;
    private final S3Client s3Client;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${minio.bucket-name:pastebin}")
    private String bucketName;
    @Value("${minio.presign-duration:1}")
    private Long presignDuration;

    public String generateUploadUrl(String uniqueUrl, Long lockedSize) {
        // define path of the file
        String objectKey = "pastes/" + uniqueUrl + ".txt";

        PutObjectRequest objectRequest = PutObjectRequest.builder().bucket(bucketName)
                .key(objectKey)
                .contentType("text/plain")
                .contentLength(lockedSize)
                .build();

        PutObjectPresignRequest putObjectPresignRequest = PutObjectPresignRequest.builder()
                .putObjectRequest(objectRequest)
                .signatureDuration(Duration.ofMinutes(presignDuration))
                .build();

        return s3Presigner.presignPutObject(putObjectPresignRequest).url().toString();
    }

    public String generateDownloadUrl(String url) {
        String objectKey = "pastes/" + url + ".txt";

        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                .getObjectRequest(objectRequest)
                .signatureDuration(Duration.ofMinutes(presignDuration))
                .build();

        return s3Presigner.presignGetObject(getObjectPresignRequest).url().toString();
    }

    @Scheduled(cron = "0 0 0 * * *") // each day at midnight
    public void flushExpiredPastes() {
        // get all expired pastes
        List<String> urls = pasteRepository.findPastesByPasteStatusIsExpired();
        for (String url : urls) {
            String objectKey = "pastes/" + url + ".txt";

            // delete from minio
            try {
                DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .build();
                s3Client.deleteObject(deleteObjectRequest);
            } catch (Exception e) {
                log.error(e.getMessage());
            }

            // delete from redis
            stringRedisTemplate.delete("paste:" + url);
            stringRedisTemplate.delete("reads:" + url);
        }
    }

}
