package io.vercy.brick.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@WebServlet(asyncSupported = true)
public class AsyncNonBlockingServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AsyncNonBlockingServlet.class);
    private static final ExecutorService receiver = Executors.newSingleThreadExecutor();
    private static final ExecutorService dispatcher = Executors.newSingleThreadExecutor();
    private static volatile int requestCounter = 0;

    static BrickParser parser = new BrickParser();
    static InternalServiceAccess internalServiceAccess = new InternalServiceAccess();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        AsyncContext asyncCtx = req.startAsync();
        dispatcher.submit(new AsyncWorker(asyncCtx));
    }

    static class AsyncWorker implements Runnable {
        final AsyncContext asyncCtx;
        final int requestId;

        int processedBrickCount;

        AsyncWorker(AsyncContext asyncCtx) {
            this.asyncCtx = asyncCtx;
            this.requestId = requestCounter++;
        }

        public void run() {
            asyncCtx.getResponse().setContentType("text/plain");
            try {
                ServletInputStream in = asyncCtx.getRequest().getInputStream();
                in.setReadListener(new ReadListener() {
                    @Override
                    public void onDataAvailable() throws IOException {
                        while (in.available() >= BrickParser.BRICK_REQUEST_BYTE_COUNT) {
                            BrickPayload brick = parser.read(in);
                            if (brick == null)
                                continue;

                            processedBrickCount++;
                            receiver.submit(() -> internalServiceAccess.sendBlocking(brick));
                        }
                    }

                    @Override
                    public void onAllDataRead() throws IOException {
                        asyncCtx.getResponse().getOutputStream().print("Public Brick Service received " + processedBrickCount + " bricks");
                        log.debug("{} > Public Brick Service received {} bricks", String.format("%08X", requestId), processedBrickCount);
                        asyncCtx.complete();
                    }

                    @Override
                    public void onError(Throwable t) {
                        handleException(t);
                        asyncCtx.complete();
                    }
                });


            } catch (IOException | RuntimeException e) {
                handleException(e);
            }
        }

        void handleException(Throwable t) {
            String message = String.format("Could not process request: cause: %s %s", t.getClass(), t.getMessage());
            try {
                asyncCtx.getResponse().getOutputStream().print(message);
            } catch (IOException eee) {
                log.error("Error during execution. Failed to respond with the error content.", eee);
            }
            log.warn("{} > {}", String.format("%08X", requestId), message);
        }
    }


}