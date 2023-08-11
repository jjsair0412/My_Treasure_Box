package com.contentsprocess.contentsprocess.generatePresignedUrl.controller;


import com.amazonaws.HttpMethod;
import com.contentsprocess.contentsprocess.generatePresignedUrl.entity.requestDomain;
import com.contentsprocess.contentsprocess.generatePresignedUrl.entity.responseDomain;
import com.contentsprocess.contentsprocess.generatePresignedUrl.service.objectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/test")
public class awsController {

    private final objectService service;

    @PutMapping("/upload")
    public ResponseEntity<responseDomain> upload(requestDomain request){
        request.setHttpMethod(HttpMethod.PUT);
        return new ResponseEntity<>(service.simpleUpload(request.getBucket(), request.getFilePath(), request.getHttpMethod()), HttpStatus.OK);
    }

}
