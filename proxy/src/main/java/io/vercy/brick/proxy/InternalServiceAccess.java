package io.vercy.brick.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;

public class InternalServiceAccess {
    private static final String INTERNAL_APP_HOST = "localhost";
    private static final int INTERNAL_APP_PORT = 8080;
    private static Charset UTF_8 = Charset.forName("UTF-8");

    private static final Logger log = LoggerFactory.getLogger(ProxyImpl_NonBlockingServlet.class);

    public String sendBlocking(BrickPayload brick) {
        try {
            Socket s = new Socket(INTERNAL_APP_HOST, INTERNAL_APP_PORT);

            OutputStream out = s.getOutputStream();
            String httpRequest = createHttpRequest(brick);
            out.write(httpRequest.getBytes(UTF_8));
            out.flush();

            InputStream in = s.getInputStream();
            String response = readHttpResponse(in);

            s.close();

            return response;
        } catch (IOException e) {
            log.warn("Blocking socket send failed.", e);
            return null;
        }
    }

    void sendNonBlocking(BrickPayload brick, CompletionHandler<String, Void> callback) {
        try {
            AsynchronousSocketChannel sockChannel = AsynchronousSocketChannel.open();
            sockChannel.connect(new InetSocketAddress(INTERNAL_APP_HOST, INTERNAL_APP_PORT), sockChannel, new CompletionHandler<Void, AsynchronousSocketChannel>() {

                @Override
                public void completed(Void result, AsynchronousSocketChannel channel) {
                    String httpRequest = createHttpRequest(brick);
                    ByteBuffer buf = ByteBuffer.wrap(httpRequest.getBytes());
                    startWrite(channel, buf, callback);
                }

                @Override
                public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                    callback.failed(new RuntimeException("Non-blocking socket connect failed", exc), null);
                }
            });
        } catch (IOException ex) {
            callback.failed(new RuntimeException("Non-blocking socket connect failed", ex), null);
        }
    }

    private static void startWrite(AsynchronousSocketChannel sockChannel, ByteBuffer buf, CompletionHandler<String, Void> callback) {
        sockChannel.write(buf, sockChannel, new CompletionHandler<Integer, AsynchronousSocketChannel>() {
            @Override
            public void completed(Integer result, AsynchronousSocketChannel channel) {
                if(buf.hasRemaining()) {
                    startWrite(sockChannel, buf, callback);
                    return;
                }

                final ByteBuffer responseBuf = ByteBuffer.allocate(512);
                startRead(channel, responseBuf, callback);
            }

            @Override
            public void failed(Throwable e, AsynchronousSocketChannel channel) {
                callback.failed(new RuntimeException("Non-blocking socket read failed", e), null);
            }
        });
    }

    private static void startRead(AsynchronousSocketChannel sockChannel, ByteBuffer buf, CompletionHandler<String, Void> callback) {
        sockChannel.read(buf, sockChannel, new CompletionHandler<Integer, AsynchronousSocketChannel>() {
            @Override
            public void completed(Integer result, AsynchronousSocketChannel channel) {
                if(result != -1) {
                    startRead(sockChannel, buf, callback);
                    return;
                }

                try {
                    channel.close();
                } catch (IOException e) {
                    // ignore this
                }

                try(InputStream in = new ByteArrayInputStream(buf.array())) {
                    String response = readHttpResponse(in);
                    callback.completed(response, null);
                } catch(IOException e) {
                    callback.failed(new RuntimeException("Non-blocking socket read failed", e), null);
                }
            }

            @Override
            public void failed(Throwable e, AsynchronousSocketChannel channel) {
                callback.failed(new RuntimeException("Non-blocking socket read failed", e), null);
            }
        });
    }

    // region HTTP codec

    enum HttpResponseState { CONTENT, FOUND_CR }

    private static String readHttpResponse(InputStream in) throws IOException {
        HttpResponseState state = HttpResponseState.CONTENT;
        try (InputStreamReader reader = new InputStreamReader(in, UTF_8)) {
            StringBuilder lineBuffer = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                if (state == HttpResponseState.CONTENT && c == '\r') {
                    state = HttpResponseState.FOUND_CR;
                    continue;
                }

                if (state == HttpResponseState.FOUND_CR && c == '\n') {
//                    log.info("> {}", lineBuffer.toString());
                    lineBuffer.setLength(0); // only keep the last line
                    state = HttpResponseState.CONTENT;
                    continue;
                }

                lineBuffer.append((char)c);
            }
            return lineBuffer.toString();
        }
    }

    String createHttpRequest(BrickPayload brick) {
        return  "GET /brick?color=" + brick.getColor() + "&length=" + brick.getLength() + " HTTP/1.1\n" +
                "Host: localhost\n" +
                "Connection: close\n" +
                "Content-Type: text/plain\n" +
                "\n";
    }

    // endregion
}
