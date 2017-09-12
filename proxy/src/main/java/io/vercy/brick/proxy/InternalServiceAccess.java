package io.vercy.brick.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;

public class InternalServiceAccess {
    private static final String INTERNAL_APP_HOST = "localhost";
    private static final int INTERNAL_APP_PORT = 8080;

    private static final Logger log = LoggerFactory.getLogger(AsyncNonBlockingServlet.class);

    void sendBlocking(int requestId, BrickPayload brick) {
        log.debug("{} > Sending brick: {color: {}, length: {}}", String.format("%08X", requestId), brick.getColor(), brick.getLength());
        HttpURLConnection cn = null;
        try {
            URL url = new URL(urlToInternalBrickSvc(brick.getColor(), brick.getLength()));
            cn = (HttpURLConnection) url.openConnection();
            cn.setRequestProperty("Content-Type", "text/plain");
            cn.setRequestMethod("GET");
            cn.setDoInput(true);
            String response = readFully(cn.getInputStream());
            int responseCode = cn.getResponseCode();
            if (responseCode != 200) {
                log.warn("{} < Failed: {}, status: {}", String.format("%08X", requestId), url, responseCode);
            } else {
                log.debug("{} > {}", String.format("%08X", requestId), response);
            }
        } catch (ConnectException ce) {
            log.warn("{} Internal Service is not running: {}", String.format("%08X", requestId), ce.getMessage());
            // return try again later status code
        } catch (IOException ex) {
            log.warn("{} Request failed", String.format("%08X", requestId), ex);
        } finally {
            if (cn != null) {
                try {
                    cn.disconnect();
                } catch (RuntimeException e) {
                    // never mind this
                }
            }
        }
    }

    static String urlToInternalBrickSvc(AnsiColor color, int length) {
        return "http://" + INTERNAL_APP_HOST + ":" + INTERNAL_APP_PORT + "/brick?color=" + color + "&length=" + length;
    }

    private static String readFully(InputStream in) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(in, Charset.forName("UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                sb.append((char) c);
            }
            return sb.toString();
        }
    }

    void sendNonBlocking(int requestId, BrickPayload brick) {
        try {
            AsynchronousSocketChannel sockChannel = AsynchronousSocketChannel.open();
            sockChannel.connect(new InetSocketAddress(INTERNAL_APP_HOST, INTERNAL_APP_PORT), sockChannel, new CompletionHandler<Void, AsynchronousSocketChannel>() {

                @Override
                public void completed(Void result, AsynchronousSocketChannel channel) {
                    String httpRequest =
                            "GET /brick?color=" + brick.getColor() + "&length=" + brick.getLength() + " HTTP/1.1\r\n" +
                                    "Host: localhost:8080\r\n" +
                                    "Content-Type: text/plain";
                    startWrite(channel, httpRequest, requestId);
                }

                @Override
                public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                    log.warn("{} Internal Service is not running: {}", String.format("%08X", requestId), exc.getMessage());
                }
            });
        } catch (IOException ex) {
            log.warn("{} Request failed", String.format("%08X", requestId), ex);
        }
    }


    private static void startRead(AsynchronousSocketChannel sockChannel, int requestId) {
        final ByteBuffer buf = ByteBuffer.allocate(128);
        sockChannel.read(buf, sockChannel, new CompletionHandler<Integer, AsynchronousSocketChannel>() {
            @Override
            public void completed(Integer result, AsynchronousSocketChannel channel) {
                String response = new String(buf.array());
                log.debug("{} > {}", String.format("%08X", requestId), response);
                try {
                    channel.close();
                } catch (IOException e) {
                    log.warn("close failed", e);
                }
            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                log.warn("fail to read message from server", exc);
            }
        });


    }

    private static void startWrite(AsynchronousSocketChannel sockChannel, String request, int requestId) {
        ByteBuffer buf = ByteBuffer.wrap(request.getBytes());
        buf.flip();
        sockChannel.write(buf, sockChannel, new CompletionHandler<Integer, AsynchronousSocketChannel>() {
            @Override
            public void completed(Integer result, AsynchronousSocketChannel channel) {
                try {
                    channel.shutdownOutput();
                } catch (IOException e) {
                    log.warn("shutdown output failed", e);
                }
                startRead(channel, requestId);
            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                log.warn("Fail to write the message to server", exc);
            }
        });
    }
}
