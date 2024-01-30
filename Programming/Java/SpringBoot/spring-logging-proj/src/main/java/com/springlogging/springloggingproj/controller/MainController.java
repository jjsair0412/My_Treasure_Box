package com.springlogging.springloggingproj.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Controller
@Slf4j
public class MainController {

    @GetMapping("/")
    public String testController(){
        log.info("init Controller");
        return "hello";
    }

}
