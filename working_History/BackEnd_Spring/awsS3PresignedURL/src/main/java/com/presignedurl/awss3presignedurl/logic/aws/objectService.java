package com.presignedurl.awss3presignedurl.logic.aws;

import com.amazonaws.HttpMethod;

import java.net.URL;
public interface objectService {
    // 업로드
    URL simpleUploadStock(String bucket, String filePath, HttpMethod httpMethod);
}
