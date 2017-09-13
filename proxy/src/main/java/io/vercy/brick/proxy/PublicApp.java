package io.vercy.brick.proxy;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.List;

public class PublicApp {
    private static final Logger log = LoggerFactory.getLogger(PublicApp.class);

    public static int PUBLIC_SVC_PORT = 8090;



    public static void main(String[] args) throws Exception {
//        DeploymentInfo servletBuilder = Servlets.deployment()
//                .setClassLoader(PublicApp.class.getClassLoader())
//                .setContextPath("/brickset")
//                .setDeploymentName("blocking-brick-proxy.war")
//                .addServlets(
//                        Servlets.servlet("BlockingServlet", AsyncNonBlockingServlet.class)
//                                .setAsyncSupported(true)
//                                .addInitParam("message", "Hello World")
//                                .addMapping("/*"));
//
//        DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
//        manager.deploy();
//        PathHandler path = Handlers.path(Handlers.redirect("/brickset"))
//                .addPrefixPath("/brickset", manager.start());
//
//        Undertow server = Undertow.builder()
//                .addHttpListener(PUBLIC_SVC_PORT, "localhost")
//                .setHandler(path)
//                .build();
//        server.start();

        BrickPayload brick = new BrickPayload(AnsiColor.CYAN, 1);
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