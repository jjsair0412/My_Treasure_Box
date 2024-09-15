package com.contentsprocess.contentsprocess.generatePresignedUrl.service;

import com.amazonaws.HttpMethod;
import com.contentsprocess.contentsprocess.generatePresignedUrl.entity.responseDomain;

public interface objectService {
    // 업로드
    responseDomain simpleUpload(String bucket, String filePath, HttpMethod httpMethod);
}
