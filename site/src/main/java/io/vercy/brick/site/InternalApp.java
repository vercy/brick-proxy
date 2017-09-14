package io.vercy.brick.site;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InternalApp {

    public static int INTERNAL_SVC_PORT = 8080;

    public static void main(String[] args) throws Exception {
//        ExecutorService dispatcher = Executors.newCachedThreadPool();
//        HttpHandler brickHandler = new BrickRequestHandler();
//        Undertow server = Undertow.builder()
//                .addHttpListener(INTERNAL_SVC_PORT, "localhost")
//                .setHandler(exchange -> {
//                    if("/brick".equals(exchange.getRequestURI())) {
//                        dispatcher.submit(() -> {
//                            try {
//                                brickHandler.handleRequest(exchange)
//                            } catch (Exception e) {
//
//                            }
//                        });
//                    } else {
//                        exchange.setStatusCode(404);
//                        exchange.getResponseSender().send("Brick Construction Site: only /brick?color={c}&length={l} requests are supported");
//                    }
//                }).build();
//        server.start();
//        dispatcher.shutdown();


        DeploymentInfo servletBuilder = Servlets.deployment()
                .setClassLoader(InternalApp.class.getClassLoader())
                .setContextPath("/brick")
                .setDeploymentName("blocking-brick-processor.war")
                .addServlets(
                        Servlets.servlet("UnlimitedBlockingServlet", UnlimitedBlockingServlet.class)
                                .setAsyncSupported(true)
                                .addInitParam("message", "Hello World")
                                .addMapping("/*"));

        DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();
        PathHandler path = Handlers.path(Handlers.redirect("/brick"))
                .addPrefixPath("/brick", manager.start());

        Undertow server = Undertow.builder()
                .addHttpListener(INTERNAL_SVC_PORT, "localhost")
                .setHandler(path)
                .build();
        server.start();
    }



}