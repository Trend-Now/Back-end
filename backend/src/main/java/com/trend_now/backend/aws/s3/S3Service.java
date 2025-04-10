package com.trend_now.backend.aws.s3;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class S3Service {
    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    /**
     * S3에 파일을 업로드하는 메서드
     * s3Key = prefix/현재시간/UUID_파일이름
     */
    public String uploadFile(MultipartFile file, String prefix, String filename) throws IOException {
        String s3Key = generateS3Key(prefix, filename);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .contentType(file.getContentType())
            .build();

        s3Client.putObject(
            putObjectRequest,
            software.amazon.awssdk.core.sync.RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );

        return "https://" + bucketName + ".s3.amazonaws.com/" + s3Key;
    }

    private String generateS3Key(String prefix, String filename) {
        return prefix + "/" + LocalDate.now() + "/" + UUID.randomUUID() + "_" + filename;
    }
}
