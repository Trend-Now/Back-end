package com.trend_now.backend.common;

import com.trend_now.backend.exception.CustomException.AesDecryptionException;
import com.trend_now.backend.exception.CustomException.AesEncryptionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AesUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom;

    public AesUtil(@Value("${jwt.refresh-token.private-key}") String privateKey) {
        // 키가 32바이트(256비트)인지 확인
        if (privateKey.length() != 32) {
            throw new IllegalArgumentException("[AesUtil 생성자] AES-256 키는 32바이트여야 합니다.");
        }

        this.secretKey = new SecretKeySpec(privateKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        this.secureRandom = new SecureRandom();
    }

    /**
     * AES-GCM 암호화 메서드
     * - 매번 새로운 IV를 생성하여 보안성을 보장
     */
    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            // 매번 새로운 IV 생성
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // IV + 암호문 결합
            byte[] encrypted = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, encrypted, 0, iv.length);
            System.arraycopy(cipherText, 0, encrypted, iv.length, cipherText.length);

            return Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);

        } catch (Exception e) {
            throw new AesEncryptionException("[AesUtil.encrypt] 암호화 실패: " + e.getMessage());
        }
    }

    /**
     * AES-GCM 복호화 메서드
     */
    public String decrypt(String encryptedText) {
        try {
            byte[] encrypted = Base64.getUrlDecoder().decode(encryptedText);

            if (encrypted.length < GCM_IV_LENGTH + GCM_TAG_LENGTH) {
                throw new IllegalArgumentException("[AesUtil.decrypt] 암호화된 데이터가 너무 짧습니다.");
            }

            // IV와 암호문 분리
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherText = new byte[encrypted.length - GCM_IV_LENGTH];

            System.arraycopy(encrypted, 0, iv, 0, iv.length);
            System.arraycopy(encrypted, iv.length, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new AesDecryptionException("[AesUtil.decrypt] 복호화 실패: " + e.getMessage());
        }
    }
}