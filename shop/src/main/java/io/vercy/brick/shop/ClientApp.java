package io.vercy.brick.shop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientApp {

    static final String PUBLIC_BRICK_SERVICE_HOST = "localhost:8090";
    static final long SEND_MILLIS_PER_BYTE = 0;
    static final Logger log = LoggerFactory.getLogger(ClientApp.class);

    static final int BRICK_SETS_TO_SEND = 10000;
    static final Throughput throughput = new Throughput(1000);

    static final AtomicInteger connectTimeouts = new AtomicInteger(0);
    static final AtomicInteger readTimeouts = new AtomicInteger(0);
    static final AtomicInteger otherIoErrors = new AtomicInteger(0);

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        log.info("Sending {} brick sets at {}", BRICK_SETS_TO_SEND, throughput);
        int sentCount = 0;
        BrickSetFactory factory = new BrickSetFactory();
        ExecutorService executor = Executors.newCachedThreadPool();

        List<Future> futures = new ArrayList<>(BRICK_SETS_TO_SEND);
        while(sentCount < BRICK_SETS_TO_SEND) {
            for(int i = 0; sentCount < BRICK_SETS_TO_SEND && i < throughput.batchSize; i++, sentCount++) {
                futures.add(executor.submit(() -> sendBrickSet(factory.nextBrickSet())));
            }

            Thread.sleep(throughput.delayMillis);

            // remove completed futures
            Iterator<Future> futureIterator = futures.iterator();
            while(futureIterator.hasNext())
                if(futureIterator.next().isDone())
                    futureIterator.remove();
        }

        log.info("Waiting for {} requests to complete", futures.size());

        // synchronize on remaining futures
        futures.forEach(f -> {
            try {
                f.get();
            } catch (ExecutionException | InterruptedException e) {
                // never mind
            }
        });

        long duration = System.currentTimeMillis() - start;
        log.info("Sent {} brick sets in {} seconds.",
                BRICK_SETS_TO_SEND, String.format("%.2f", duration / 1000f));
        log.info("  target: {}, actual throughput: {}",
                throughput, String.format("%.2f", BRICK_SETS_TO_SEND / (duration / 1000.0)));
        log.info("  connectTimeouts: {}, readTimeouts: {}, otherIoErrors: {}",
                connectTimeouts.get(), readTimeouts.get(), otherIoErrors.get());

        executor.shutdown();
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
                out.flush();
                Thread.sleep(SEND_MILLIS_PER_BYTE);

                byte length = bytes[i + 1];
                log.info("< length: {}", length);
                out.write('0' + length);
                out.flush();
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
            connectTimeouts.incrementAndGet();
            log.warn("Internal Service is not running: {}", ce.getMessage());
            // return try again later status code
        } catch(SocketTimeoutException ex) {
            readTimeouts.incrementAndGet();
        } catch(IOException ex) {
            otherIoErrors.incrementAndGet();
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

    static class Throughput {
        int requestPerSecond;
        long delayMillis;
        int batchSize;

        Throughput(int requestPerSecond) {
            this.requestPerSecond = requestPerSecond;
            batchSize = 1;
            while(requestPerSecond / batchSize > 100)
                batchSize++;
            delayMillis = (long)(1000 / ((float)requestPerSecond / batchSize));
        }

        @Override
        public String toString() {
            return "" + requestPerSecond + " / second";
        }
    }
}