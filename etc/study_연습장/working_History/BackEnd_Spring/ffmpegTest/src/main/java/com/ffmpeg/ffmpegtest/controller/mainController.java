package com.ffmpeg.ffmpegtest.controller;

import com.ffmpeg.ffmpegtest.service.ffmpegCli;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class mainController {

    @Autowired private final ffmpegCli service;

    @GetMapping("/test")
    public void testEncoding(){
        service.ffmpegCliTest();
    }
}
