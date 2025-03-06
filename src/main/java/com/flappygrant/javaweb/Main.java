package com.flappygrant.javaweb;

import com.flappygrant.javaweb.server.Server;
import com.flappygrant.javaweb.utils.LoggerSystem;

public class Main {
    // Initializes the logger system
    private static final LoggerSystem logger = new LoggerSystem("server");

    // Creates a variable to store the web server instance
    private static Server webServer;

    public static void main(String[] args) throws Exception {
        // Initializes the web server
        webServer = new Server("http://", "0.0.0.0", 80);
        webServer.start();
    }

    // Allows other classes to access the logger system
    public static LoggerSystem getLogger() {
        return logger;
    }
}
