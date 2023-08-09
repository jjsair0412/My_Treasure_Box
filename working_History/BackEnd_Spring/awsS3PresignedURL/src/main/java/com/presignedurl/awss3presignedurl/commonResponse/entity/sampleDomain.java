package com.presignedurl.awss3presignedurl.commonResponse.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import com.presignedurl.awss3presignedurl.commonResponse.entity.userInfo;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class sampleDomain {

    private String successResult;
    private HttpStatus status;
    private userInfo userInfo;



    @Builder
    public sampleDomain(HttpStatus status, String successResult, userInfo userInfo) {
        this.successResult = successResult;
        this.userInfo = userInfo;
        this.status = status;
    }
}
