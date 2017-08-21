package io.vercy.brick.site;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;

public class BrickSiteMain {

    public static void main(String[] args) {
        HttpHandler brickHandler = new BrickRequestHandler();
        Undertow server = Undertow.builder()
                .addHttpListener(8080, "localhost")
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