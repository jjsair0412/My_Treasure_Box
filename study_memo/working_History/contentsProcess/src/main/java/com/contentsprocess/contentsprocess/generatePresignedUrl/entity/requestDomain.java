package com.contentsprocess.contentsprocess.generatePresignedUrl.entity;

import com.amazonaws.HttpMethod;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class requestDomain {

    private String bucket;
    private String filePath;
    private HttpMethod httpMethod;


    @Builder
    public requestDomain(String bucket, String filePath) {
        this.bucket = bucket;
        this.filePath = filePath;
    }
}
