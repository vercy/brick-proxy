package io.vercy.brick.site;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@WebServlet(asyncSupported = true)
public class UnlimitedBlockingServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(UnlimitedBlockingServlet.class);
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static volatile int requestCounter = 0;

    private static final long BRICK_PROCESS_MILLIS = 100L;

    private static void waitForProcessingBrick(BrickPayload brick) {
        try {
            Thread.sleep(BRICK_PROCESS_MILLIS * brick.getLength());
        } catch(InterruptedException ex) {
            // ignore this
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        AsyncContext asyncCtx = req.startAsync();
        executor.submit(new AsyncWorker(asyncCtx));
    }

    static class AsyncWorker implements Runnable {
        AsyncContext asyncCtx;

        AsyncWorker(AsyncContext asyncCtx) {
            this.asyncCtx = asyncCtx;
        }

        public void run() {
            int requestId = requestCounter++;
            String color = null;
            String length = null;
            try {
                color = asyncCtx.getRequest().getParameter("color");
                length = asyncCtx.getRequest().getParameter("length");
                BrickPayload brick = BrickPayload.parse(color, length);

                log.trace("{} > {}", String.format("%08X", requestId), brick);
                waitForProcessingBrick(brick);
                asyncCtx.getResponse().getOutputStream().print("Brick Construction Site: received " + brick);
                log.info("{} > {}", String.format("%08X", requestId), brick.render());
            } catch(IOException | RuntimeException e) {
                String message = String.format("Could not process request: [color=%s, length=%s], cause: %s %s", color, length, e.getClass(), e.getMessage());
                try {
                    asyncCtx.getResponse().getOutputStream().print(message);
                } catch(IOException eee) {
                    log.error("Error during execution. Failed to respond with the error content.",eee);
                }
                log.warn("{} > {}", String.format("%08X", requestId), message);
            } finally {
                asyncCtx.complete();
            }
        }
    }
}
