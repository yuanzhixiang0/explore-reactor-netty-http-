package org.example;

import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Hooks;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws InterruptedException {

//        Hooks.onOperatorDebug();

        HttpClient httpClient = HttpClient
                .create(ConnectionProvider.builder("pushv5-webClient")
                        .maxIdleTime(Duration.ofMillis(3))
                        .build())
                .responseTimeout(Duration.ofSeconds(3));

        WebClient webClient = WebClient
                .builder()
                .baseUrl("http://127.0.0.1:8080")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        webClient.get()
                .uri("/inner/v3/users/api-secret/single-detail/{apiKey}?uRealReqIP={ip}", "123", "123")
                .retrieve()
                .toEntity(Map.class)
                .subscribe(System.out::println, Throwable::printStackTrace);

        TimeUnit.SECONDS.sleep(60);
    }

}
