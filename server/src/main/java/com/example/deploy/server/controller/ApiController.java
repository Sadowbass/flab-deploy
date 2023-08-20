package com.example.deploy.server.controller;

import com.example.deploy.server.config.TargetServers;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class ApiController {

    private final TargetServers targetServers;

    @GetMapping("/api")
    public String api() {
        RestTemplate restTemplate = new RestTemplate();
        String server = targetServers.getServer();
        return restTemplate.getForObject(URI.create(server + "/api"), String.class);
    }
}
