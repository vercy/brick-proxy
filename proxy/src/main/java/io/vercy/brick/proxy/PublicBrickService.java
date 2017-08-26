package io.vercy.brick.proxy;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.BlockingHandler;

public class PublicBrickService {

    public static int PUBLIC_SVC_PORT = 8090;

    public static void main(String[] args) {
        HttpHandler brickSetHandler = new BlockingHandler(new BlockingBrickHandler());
        Undertow server = Undertow.builder()
                .addHttpListener(PUBLIC_SVC_PORT, "localhost")
                .setHandler(exchange -> {
                    if("/brickset".equals(exchange.getRequestURI())) {
                        brickSetHandler.handleRequest(exchange);
                    } else {
                        exchange.setStatusCode(404);
                        exchange.getResponseSender().send("Public Brick Service: (POST) /brickset, content: [%3x]...");
                    }
                }).build();
        server.start();
    }
}