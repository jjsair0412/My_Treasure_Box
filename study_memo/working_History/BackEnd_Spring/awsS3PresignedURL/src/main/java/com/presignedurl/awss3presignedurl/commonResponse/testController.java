package com.presignedurl.awss3presignedurl.commonResponse;

import com.presignedurl.awss3presignedurl.commonResponse.entity.sampleDomain;
import com.presignedurl.awss3presignedurl.commonResponse.entity.userInfo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class testController {

    @GetMapping("/test")
    public ResponseEntity<sampleDomain> test() {
        return new ResponseEntity<>(makeResponse(), HttpStatus.OK);
    }

    private sampleDomain makeResponse(){

        userInfo user = new userInfo();
        user.setAge(27);
        user.setName("jinseong");

        return sampleDomain.builder()
                .successResult("true")
                .status(HttpStatus.OK)
                .userInfo(user)
                .build();
    }

}
