package com.usermanager.usermanager.Controller.Main;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.usermanager.usermanager.Domain.DTO.MainDomain;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class MainPage {
    
    @Value("${server.port}")
    private String port;


    @GetMapping("/user")
    public String mainpageString(
            @ModelAttribute MainDomain mainDomain,
            @RequestParam(required = false, defaultValue = "1") int defaultValue, 
            Model model
        ){
        model.addAttribute("default", defaultValue);
        model.addAttribute("hello", mainDomain.getHello());
        model.addAttribute("port", port);
        return "hello";
    }

    @GetMapping("/start")
    @ResponseBody
    public String startMappString(){
        log.info("Here is usermanager PROJECT START METHOD");
        return "hi im usermanager start";
    }
}
