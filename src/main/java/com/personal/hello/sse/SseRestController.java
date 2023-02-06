package com.personal.hello.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@Slf4j
public class SseRestController {

    Map<UUID, SubscriptionData> subscriptions = new ConcurrentHashMap<>(); // 1

    @GetMapping(path = "/open-sse-stream/{nickName}", produces = MediaType.TEXT_EVENT_STREAM_VALUE) // 2
    public Flux<ServerSentEvent> openSseStream(@PathVariable String nickName) {

        return Flux.create(fluxSink -> { // 3
            log.info("create subscription for " + nickName);

            UUID uuid = UUID.randomUUID();

            fluxSink.onCancel( // 4
                    () -> {
                        subscriptions.remove(uuid);
                        log.info("subscription " + nickName + " was closed");
                    }
            );

            SubscriptionData subscriptionData = new SubscriptionData(nickName, fluxSink);
            subscriptions.put(uuid, subscriptionData);

            // 5
            ServerSentEvent<String> helloEvent = ServerSentEvent.builder("Hello " + nickName).build();
            fluxSink.next(helloEvent);
        });
    }

    @PutMapping(path = "/send-message-for-all")
    public void sendMessageForAll(@RequestBody SendMessageRequest request) {

        // 1
        ServerSentEvent<String> event = ServerSentEvent
                .builder(request.getMessage())
                .build();

        // 2
        subscriptions.forEach((uuid, subscriptionData) ->
                subscriptionData.getFluxSink().next(event)
        );
    }

    @PutMapping(path = "/send-message-by-name/{nickName}")
    public void sendMessageByName(
            @PathVariable String nickName,
            @RequestBody SendMessageRequest request
    ) {

        ServerSentEvent<String> event = ServerSentEvent
                .builder(request.getMessage())
                .build();

        subscriptions.forEach((uuid, subscriptionData) -> {
                    if (nickName.equals(subscriptionData.getNickName())) {
                        subscriptionData.getFluxSink().next(event);
                    }
                }
        );
    }
}
