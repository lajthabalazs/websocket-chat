package ca.lajtha;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Injector injector = Guice.createInjector(new ServerModule());
        WebSocketServer server = injector.getInstance(WebSocketServer.class);
        server.start();
    }
}
