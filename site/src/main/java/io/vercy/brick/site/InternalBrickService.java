package io.vercy.brick.site;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;

public class InternalBrickService {

    public static int INTERNAL_SVC_PORT = 8080;

    public static void main(String[] args) {
        HttpHandler brickHandler = new BrickRequestHandler();
        Undertow server = Undertow.builder()
                .addHttpListener(INTERNAL_SVC_PORT, "localhost")
                .setHandler(exchange -> {
                    if("/brick".equals(exchange.getRequestURI())) {
                        brickHandler.handleRequest(exchange);
                    } else {
                        exchange.setStatusCode(404);
                        exchange.getResponseSender().send("Brick Construction Site: only /brick?color={c}&length={l} requests are supported");
                    }
                }).build();
        server.start();
    }



}