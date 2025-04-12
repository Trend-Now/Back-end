package com.trend_now.backend.aws.s3.application;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Getter
    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    /**
     * S3에 파일을 업로드하는 메서드 s3Key = prefix/현재시간/UUID_파일이름
     */
    public String uploadFile(MultipartFile file, String prefix, String filename)
        throws IOException {
        String s3Key = generateS3Key(prefix, filename);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .contentType(file.getContentType())
            .build();

        s3Client.putObject(
            putObjectRequest,
            RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );
        return s3Key;
//        return "https://" + bucketName + ".s3.amazonaws.com/" + s3Key;
    }

    /**
     * S3에 HTTP 요청을 보내서 여러 파일을 한 번에 삭제하는 메서드
     * @param s3Keys 삭제할 S3 객체 key 목록
     */
    public void deleteFiles(List<String> s3Keys) {
        // key 목록을 ObjectIdentifier 객체 리스트로 변환
        List<ObjectIdentifier> objectsToDelete = s3Keys.stream()
            .map(key -> ObjectIdentifier.builder().key(key).build())
            .toList();

        // S3 다중 삭제 요구 형식을 맞추기 위해, ObjectIdentifier 리스트를 Delete 객체로 감싸준다
        Delete delete = Delete.builder()
            .objects(objectsToDelete)
            .build();

        // 삭제 요청 객체 생성
        DeleteObjectsRequest request = DeleteObjectsRequest.builder()
            .bucket(bucketName)
            .delete(delete)
            .build();

        s3Client.deleteObjects(request);
    }

    private String generateS3Key(String prefix, String filename) {
        return prefix + "/" + LocalDate.now() + "/" + UUID.randomUUID() + "_" + filename;
    }
}
