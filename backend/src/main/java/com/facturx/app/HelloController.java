package com.facturx.app;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

//the controller is the one that handle communication with the outside world 
@RestController
public class HelloController {

    @GetMapping("/api/healthcheck")
    public String hello() {
        return "Yess i'm working ";
    }
}