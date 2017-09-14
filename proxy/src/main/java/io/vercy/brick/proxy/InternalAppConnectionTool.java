package io.vercy.brick.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.List;

public class InternalAppConnectionTool {

    private static final Logger log = LoggerFactory.getLogger(PublicApp.class);

    public static void main(String[] args) {
        BrickPayload brick = new BrickPayload();
        brick.setColor(BrickColor.CYAN);
        brick.setLength(1);

        log.info("Blocking response: {}", sendBlocking(brick));
        log.info("Non-blocking response: {}", sendNonBlocking(brick));
    }

    static String sendBlocking(BrickPayload brick) {
        return new InternalServiceAccess().sendBlocking(brick);
    }

    static String sendNonBlocking(BrickPayload brick) {
        List<String> response = new ArrayList<>();
        // need a thread to keep the main function alive until non-blocking I/O completes
        Thread t = new Thread(() -> {
            final Object callBackLock = "lock";

            new InternalServiceAccess().sendNonBlocking(brick, new CompletionHandler<String, Void>() {
                @Override
                public void completed(String result, Void attachment) {
                    response.add(result);
                    synchronized (callBackLock) {
                        callBackLock.notify();
                    }
                }

                @Override
                public void failed(Throwable exc, Void attachment) {
                    response.add(exc.getMessage());
                    synchronized (callBackLock) {
                        callBackLock.notify();
                    }
                }
            });

            // wait for the non-blocking operation to complete
            synchronized (callBackLock) {
                try {
                    callBackLock.wait();
                } catch (InterruptedException ie) {
                    response.add(ie.getMessage());
                }
            }
        });
        t.start();
        try {
            t.join();
        } catch (InterruptedException ie) {
            return ie.getMessage();
        }

        return response.get(0);
    }
}
