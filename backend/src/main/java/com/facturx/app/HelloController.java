package com.facturx.app;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

//the controller is the one that handle communication with the outside world 
@CrossOrigin(origins = {
    "http://localhost:5173",
    "http://127.0.0.1:5173"
})
@RestController
public class HelloController {

    @GetMapping("/api/healthcheck")
    public String hello() {
        return "Yess i'm working ";
    }
}