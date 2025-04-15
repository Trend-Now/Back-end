package com.trend_now.backend.aws.s3.domain;

import com.trend_now.backend.config.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@NoArgsConstructor
public abstract class S3File extends BaseEntity {

    @Id
    @GeneratedValue
    private Long id;

    private String s3Key;

    public S3File(String s3key) {
        this.s3Key = s3key;
    }
}
