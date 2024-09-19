package com.presignedurl.awss3presignedurl.logic.aws;

import com.presignedurl.awss3presignedurl.logic.aws.config.S3Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.amazonaws.HttpMethod;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class objectServiceImpl implements objectService {

    private final S3Config amazonS3;

    // 간단버전
    @Override
    public URL simpleUpload(String bucket, String filePath, HttpMethod httpMethod) {
        return amazonS3.getAmazonS3Client().generatePresignedUrl(bucket,filePath,getPreSignedUrlExpiration(),httpMethod);
    }


    /**
     * URL timout 시간 생성
     *
     * expTimeMillis += 1000 * 60 * 30;
     * 1000 : 1초를 밀리초로 변환
     * 60 : 1분을 밀리초로 변환
     * 30 : 30분을 밀리초로 변환
     *
     * 따라서 현재 설정은 expTimeMillis에 현재 시간을 기준으로 1(180초)분 후의 밀리초 동안 유효함
     */
    private Date getPreSignedUrlExpiration() {
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000 * 60 * 1;
        expiration.setTime(expTimeMillis);
        return expiration;
    }


}
