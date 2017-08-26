package io.vercy.brick.shop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PublicBrickServiceClient {

    private static final String PUBLIC_BRICK_SERVICE_HOST = "localhost:8090";
    private static final long SEND_MILLIS_PER_BYTE = 100;
    private static final Logger log = LoggerFactory.getLogger(PublicBrickServiceClient.class);
    private static final int WORKER_COUNT = 4;
    private static final int BRICK_SETS_TO_SEND = 100;

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        log.info("Sending {} brick sets...", BRICK_SETS_TO_SEND);
        int sentCount = 0;
        BrickSetFactory factory = new BrickSetFactory();
        ExecutorService executor = Executors.newCachedThreadPool();
        while(sentCount < BRICK_SETS_TO_SEND) {
            List<Future> futures = new ArrayList<>(WORKER_COUNT);
            for(int i = 0; i < WORKER_COUNT && sentCount < BRICK_SETS_TO_SEND; i++, sentCount++) {
                futures.add(executor.submit(() -> sendBrickSet(factory.nextBrickSet())));
            }

            futures.forEach(f -> {
                try {
                    f.get();
                } catch (ExecutionException | InterruptedException e) {
                    // never mind
                }
            });
        }
        long duration = System.currentTimeMillis() - start;
        log.info("Sent {} brick sets in {} seconds", BRICK_SETS_TO_SEND, String.format("%.2f", duration / 1000f));
    }

    private static void sendBrickSet(byte[] bytes) {
        HttpURLConnection cn = null;
        try {
            URL url = new URL(urlToPublicBrickSvc());
            cn = (HttpURLConnection) url.openConnection();
            cn.setRequestProperty("Content-Type", "text/plain");
            cn.setRequestMethod("POST");
            cn.setDoInput(true);
            cn.setDoOutput(true);

            log.info("brick count: {}", bytes.length >> 1);
            OutputStream out = cn.getOutputStream();
            for(int i = 0;i + 1 < bytes.length;i += 2) {
                byte color = bytes[i];
                log.info("< color: {}", color);
                out.write('0' + color);
                Thread.sleep(SEND_MILLIS_PER_BYTE);

                byte length = bytes[i + 1];
                log.info("< length: {}", length);
                out.write('0' + length);
                Thread.sleep(SEND_MILLIS_PER_BYTE);
            }
            out.close();

            String response = readFully(cn.getInputStream());
            int responseCode = cn.getResponseCode();
            if (responseCode != 200) {
                log.warn("< Failed: {}, status: {}", url, responseCode);
            } else {
                log.info("> {}", response);
            }
        } catch (ConnectException ce) {
            log.warn("Internal Service is not running: {}", ce.getMessage());
            // return try again later status code
        } catch(IOException ex) {
            log.warn("Request failed", ex);
        } catch(InterruptedException ie) {
            log.warn("Request interrupted", ie);
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

    static String urlToPublicBrickSvc() {
        return "http://" + PUBLIC_BRICK_SERVICE_HOST + "/brickset";
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