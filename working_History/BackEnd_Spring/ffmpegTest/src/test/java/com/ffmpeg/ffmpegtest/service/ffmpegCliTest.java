package com.ffmpeg.ffmpegtest.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ffmpegCliTest {

    @Autowired private ffmpegCli service;

    @Test
    void 메인_서비스(){
        service.ffmpegCliTest();
    }


}