package io.vercy.brick.site;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.Map;

public class BrickRequestHandler implements HttpHandler {

    private static final long BRICK_PROCESS_MILLIS = 100L;
    private static final Logger log = LoggerFactory.getLogger(BrickRequestHandler.class);
    private static volatile int requestCounter = 0;

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        int requestId = requestCounter++;
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        Map<String, Deque<String>> query = exchange.getQueryParameters();
        String color = null;
        String length = null;
        try {
            color = first(query, "color");
            length = first(query, "length");
            BrickPayload brick = BrickPayload.parse(color, length);

            log.info("{} > {} ...", String.format("%08X", requestId), brick);
            waitForProcessingBrick(brick);
            exchange.getResponseSender().send("Brick Construction Site: received " + brick);
            log.info("{} > {}", String.format("%08X", requestId), brick.render());
        } catch(RuntimeException e) {
            exchange.setStatusCode(500);
            String message = String.format("Could not process request: [color=%s, length=%s], cause: %s %s", color, length, e.getClass(), e.getMessage());
            exchange.getResponseSender().send(message);
            log.warn("{} > {}", String.format("%08X", requestId), message);
        }
    }

    private String first(Map<String, Deque<String>> query, String key) {
        if(query == null || key == null)
            return null;

        Deque<String> values = query.get(key);
        if(values == null || values.size() == 0)
            return null;

        return values.getFirst();
    }

    private void waitForProcessingBrick(BrickPayload brick) {
        try {
            Thread.sleep(BRICK_PROCESS_MILLIS * brick.getLength());
        } catch(InterruptedException ex) {
            // ignore this
        }
    }
}
