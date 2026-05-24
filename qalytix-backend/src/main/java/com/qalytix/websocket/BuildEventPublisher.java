package com.qalytix.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BuildEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishBuildEvent(BuildEventPayload payload) {
        String destination = "/topic/org/" + payload.orgId() + "/builds";
        try {
            messagingTemplate.convertAndSend(destination, payload);
            log.debug("Published build event to {} — build #{} status={}", destination, payload.buildNumber(), payload.status());
        } catch (Exception e) {
            log.warn("Failed to publish build event to {}: {}", destination, e.getMessage());
        }
    }
}
