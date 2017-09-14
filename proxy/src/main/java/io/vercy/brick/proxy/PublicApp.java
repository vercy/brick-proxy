package io.vercy.brick.proxy;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;

public class PublicApp {

    public static void main(String[] args) throws Exception {
        DeploymentInfo servletBuilder = Servlets.deployment()
                .setClassLoader(PublicApp.class.getClassLoader())
                .setContextPath("/brickset")
                .setDeploymentName("blocking-brick-proxy.war")
                .addServlets(
                        Servlets.servlet("BlockingServlet", ProxyImpl_UnlimitedBlockingServlet.class)
                                .setAsyncSupported(true)
                                .addInitParam("message", "Hello World")
                                .addMapping("/*"));

        DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();
        PathHandler path = Handlers.path(Handlers.redirect("/brickset"))
                .addPrefixPath("/brickset", manager.start());

        Undertow server = Undertow.builder()
                .addHttpListener(Constants.PUBLIC_APP_PORT, "localhost")
                .setHandler(path)
                .build();
        server.start();
    }

}