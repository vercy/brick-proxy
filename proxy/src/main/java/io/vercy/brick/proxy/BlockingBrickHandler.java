package io.vercy.brick.proxy;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

public class BlockingBrickHandler implements HttpHandler {
    private static final String INTERNAL_BRICK_SERVICE_HOST = "localhost:8080";

    private static final Logger log = LoggerFactory.getLogger(BlockingBrickHandler.class);
    private static volatile int requestCounter = 0;

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        int requestId = requestCounter++;
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        int rawColor = -1;
        int rawLength = -1;
        int received = 0;
        try {
            exchange.startBlocking();
            InputStream in = exchange.getInputStream();
            while(true) {
                rawColor = in.read();
                if(rawColor < 0)
                    break;
                rawLength = in.read();
                if(rawLength < 0)
                    break;

                received++;
                AnsiColor bColor = AnsiColor.parse(rawColor - '0');
                int bLength = rawLength - '0';
                if(bColor == null || bLength <= 0) {
                    log.warn("{} > Skipping invalid brick: {color: {}, length: {}} colorByte: {}, lengthByte: {}", String.format("%08X", requestId), bColor, bLength, rawColor, rawLength);
                    continue;
                }
                log.info("{} > Sending brick: {color: {}, length: {}}", String.format("%08X", requestId), bColor, bLength);

                sendBrick(requestId, bColor, bLength);
            }
            exchange.getResponseSender().send("Public Brick Service received " + received + " bricks");
            log.info("{} > Public Brick Service received {} bricks", String.format("%08X", requestId), received);
        } catch(RuntimeException e) {
            exchange.setStatusCode(500);
            String message = String.format("Could not process request: [color=%s, length=%s], cause: %s %s", rawColor, rawLength, e.getClass(), e.getMessage());
            exchange.getResponseSender().send(message);
            log.warn("{} > {}", String.format("%08X", requestId), message);
        }
    }

    private static void sendBrick(int requestId, AnsiColor color, int length) {
        HttpURLConnection cn = null;
        try {
            URL url = new URL(urlToInternalBrickSvc(color, length));
            cn = (HttpURLConnection) url.openConnection();
            cn.setRequestProperty("Content-Type", "text/plain");
            cn.setRequestMethod("GET");
            cn.setDoInput(true);
            String response = readFully(cn.getInputStream());
            int responseCode = cn.getResponseCode();
            if (responseCode != 200) {
                log.warn("{} < Failed: {}, status: {}", String.format("%08X", requestId), url, responseCode);
            } else {
                log.info("{} > {}", String.format("%08X", requestId), response);
            }
        } catch (ConnectException ce) {
            log.warn("{} Internal Service is not running: {}", String.format("%08X", requestId), ce.getMessage());
            // return try again later status code
        } catch(IOException ex) {
            log.warn("{} Request failed", String.format("%08X", requestId), ex);
        } finally {
            if(cn != null) {
                try {
                    cn.disconnect();
                } catch (RuntimeException e) {
                    // never mind this
                }
            }
        }
    }

    static String urlToInternalBrickSvc(AnsiColor color, int length) {
        return "http://" + INTERNAL_BRICK_SERVICE_HOST + "/brick?color=" + color + "&length=" + length;
    }

    private static String readFully(InputStream in) throws IOException {
        try(InputStreamReader reader = new InputStreamReader(in, Charset.forName("UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            int c;
            while((c = reader.read()) != -1) {
                sb.append((char)c);
            }
            return sb.toString();
        }
    }
}
