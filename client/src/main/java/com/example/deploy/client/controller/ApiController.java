package com.example.deploy.client.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiController {

    @GetMapping("/api")
    public String api() {
        System.out.println("api v1");
        return "api v1";
    }

    @GetMapping("/ping")
    public String ping() {
        System.out.println("ping");
        return "ping";
    }
}
