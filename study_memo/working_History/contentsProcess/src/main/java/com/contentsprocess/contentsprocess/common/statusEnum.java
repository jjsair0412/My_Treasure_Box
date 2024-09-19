package com.contentsprocess.contentsprocess.common;


import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum statusEnum {


    SUCCESS(HttpStatus.OK,"성공 !");

    private HttpStatus statusCode;
    private String responseMessage;

    private statusEnum(HttpStatus statusCode , String responseMessage){
        this.statusCode = statusCode;
        this.responseMessage = responseMessage;
    }
}
