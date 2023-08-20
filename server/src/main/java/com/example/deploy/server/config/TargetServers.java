package com.example.deploy.server.config;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.LinkedList;
import java.util.Queue;

@Component
public class TargetServers {

    private final TargetProperties targetProperties;
    private final RestTemplate restTemplate;
    private final Queue<String> enableQueue;

    public TargetServers(TargetProperties targetProperties, RestTemplate restTemplate) {
        this.targetProperties = targetProperties;
        this.restTemplate = restTemplate;
        this.enableQueue = new LinkedList<>();
    }

    @Scheduled(fixedDelay = 1000L)
    private void ping() throws InterruptedException {
        for (Integer port : targetProperties.getPorts()) {
            int failCount = 0;
            String serverPath = targetProperties.getHost() + ":" + port;
            URI url = URI.create(serverPath + "/ping");

            while (failCount < 3) {
                try {
                    restTemplate.getForObject(url, String.class);
                    break;
                } catch (Exception e) {
                    failCount++;
                }

                Thread.sleep(1000);
            }

            if (failCount >= 3) {
                enableQueue.remove(serverPath);
            } else if (!enableQueue.contains(serverPath)) {
                enableQueue.add(serverPath);
            }
        }
    }

    public String getServer() {
        String server;

        synchronized (enableQueue) {
            if (enableQueue.isEmpty()) {
                throw new RuntimeException("no enabled servers");
            }

            server = enableQueue.poll();
            enableQueue.add(server);
        }

        return server;
    }
}
