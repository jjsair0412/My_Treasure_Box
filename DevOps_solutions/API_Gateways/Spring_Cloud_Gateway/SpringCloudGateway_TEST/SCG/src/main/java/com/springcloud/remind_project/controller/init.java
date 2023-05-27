package com.springcloud.remind_project.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class init {

    @Value("${server.port}")
    private String port;

    @RequestMapping("/check")
    public String PortCheck(Model model){
        model.addAttribute("port_number", port);
        return "hello";
    }
}   
