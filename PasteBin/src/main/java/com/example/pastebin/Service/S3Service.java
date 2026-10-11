package com.example.pastebin.Service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class S3Service {

    private final S3Presigner s3Presigner;

    @Value("${minio.bucket-name:pastebin}")
    private String bucketName;
    @Value("${minio.presign-duration:1}")
    private Long presignDuration;

    public S3Service(S3Presigner s3Presigner) {
        this.s3Presigner = s3Presigner;
    }

    public String generateUploadUrl(String uniqueUrl, Long lockedSize) {
        // define path of the file
        String objectKey = "pastes/" + uniqueUrl + ".txt";

        PutObjectRequest objectRequest = PutObjectRequest.builder().
                                            bucket(bucketName)
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
}
