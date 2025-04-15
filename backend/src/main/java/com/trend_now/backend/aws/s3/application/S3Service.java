package com.trend_now.backend.aws.s3.application;

import com.trend_now.backend.exception.CustomException.S3FileUploadException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class S3Service {

    private final S3Client s3Client;

    private final String S3_FILE_UPLOAD_FAIL = "S3 파일 업로드에 실패했습니다.";

    @Getter
    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    /**
     * S3에 파일을 업로드하는 메서드
     * @param file 업로드할 파일
     * @param prefix S3에 저장할 경로의 최상위 경로
     * @param filename 파일 이름
     * @return S3에 저장된 파일의 key
     */
    public String uploadFile(MultipartFile file, String prefix, String filename) {
        String s3Key = generateS3Key(prefix, filename);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .contentType(file.getContentType())
            .build();

        try {
            s3Client.putObject(
                putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException e) {
            throw new S3FileUploadException(S3_FILE_UPLOAD_FAIL);
        }
        return s3Key;
    }

    /**
     * S3 단일 파일을 삭제하는 메서드
     * @param s3Key 삭제할 S3 객체 key
     */
    public void deleteFile(String s3Key) {
        s3Client.deleteObject(builder -> builder.bucket(bucketName).key(s3Key));
    }

    /**
     * S3 여러 파일을 한 번에 삭제하는 메서드
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

    /**
     * S3에 저장할 파일의 key를 생성하는 메서드
     * prefix/현재시간/UUID/파일이름
     */
    private String generateS3Key(String prefix, String filename) {
        return prefix + LocalDate.now() + "/" + UUID.randomUUID() + "_" + filename;
    }
}
