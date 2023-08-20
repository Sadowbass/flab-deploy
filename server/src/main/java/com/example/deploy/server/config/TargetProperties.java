package com.example.deploy.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@ConfigurationProperties(prefix = "msa")
public class TargetProperties {

    private final List<Integer> ports;
    private final String host;

    @ConstructorBinding
    public TargetProperties(List<Integer> ports, String host) {
        this.ports = ports;
        this.host = host;
    }

    @Bean
    public RestTemplate pingRestTemplate() {
        return new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(1L))
                .setReadTimeout(Duration.ofSeconds(1L))
                .build();
    }

    public List<Integer> getPorts() {
        return Collections.unmodifiableList(this.ports);
    }

    public String getHost() {
        return this.host;
    }
}
