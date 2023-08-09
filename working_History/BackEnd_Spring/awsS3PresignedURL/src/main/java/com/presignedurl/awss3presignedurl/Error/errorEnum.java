package com.presignedurl.awss3presignedurl.Error;

import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;

@Getter
@ToString
public enum errorEnum {

    FIRST_ERROR("E01",HttpStatus.BAD_REQUEST,"http bad request 에러 발생 ! - 400 에러"),
    SECOND_ERROR("E02", HttpStatus.BAD_GATEWAY,"http bad gateway 에러 발생 ! - 502 에러"),
    THIRD_ERROR("E02", HttpStatus.INTERNAL_SERVER_ERROR,"http bad gateway 에러 발생 ! - 500 에러");

    private String ErrorCode;
    private HttpStatus status;
    private String ErrorMessage;

    errorEnum(String errorCode, HttpStatus status, String errorMessage) {
        ErrorCode = errorCode;
        this.status = status;
        ErrorMessage = errorMessage;
    }
}
