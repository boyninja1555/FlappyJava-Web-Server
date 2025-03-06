package com.flappygrant.javaweb.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.flappygrant.javaweb.Main;
import com.flappygrant.javaweb.utils.LoggerSystem;

public class Server {
    // Gets the logger system
    private final LoggerSystem LOGGER = Main.getLogger();

    // Creates variables to store the paths we need
    private Path serverRoot;
    private Path backendRoot;

    // Creates variables to store the host information
    private String protocol;
    private String host;
    private int port;

    // Creates variables to store the server's states
    private boolean active = false;
    private List<Route> routes = new ArrayList<>();

    public Server(String _protocol, String _host, int _port) throws Exception {
        // Initializes dynamic variables
        protocol = _protocol;
        host = _host;
        port = _port;
        serverRoot = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
        backendRoot = Path.of(serverRoot.toString(), "backend");
    }

    public void start() {
        LOGGER.info("Starting FlappyJava Web Server...");

        init();
        parseBindInfo();
        parseRoutes();

        // Starts the server
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            active = true;
            LOGGER.info("Server started! Visit " + protocol + host + ":" + port + " to access the result.");

            while (active) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            LOGGER.error("Server error: " + e.getMessage());
        }

        // Stops the server if it stops, or if an error occures
        stop();
    }

    public void stop() {
        LOGGER.info("Stopping FlappyJava Web Server...");
        active = false;
        LOGGER.info("Server stopped!");
    }

    public void init() {
        // Initializes the server's files
        File serverDir = new File(serverRoot.toString());
        File stateFile = new File(Path.of(serverDir.toString(), "fjws.state").toString());

        // Does some checks to determine if the server is already initialized
        if (stateFile.exists()) {
            boolean doInit = false;

            try (BufferedReader reader = new BufferedReader(new FileReader(stateFile))) {
                String line = reader.readLine();

                if (line != null) {
                    String[] keyValue = line.split("=");

                    if (keyValue.length == 2) {
                        String key = keyValue[0];
                        String value = keyValue[1];

                        if (key.equals("already-initialized")) {
                            doInit = value.equals("false");
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Failed to read state file: " + e.getMessage());
            }

            if (!doInit)
                return;
        }

        LOGGER.info("Initializing FlappyJava Web Server...");

        File backendDir = new File(backendRoot.toString());

        if (!backendDir.exists()) {
            backendDir.mkdir();
        }

        List<String> resourceFiles = List.of("/backend-files/javaweb.xml");

        for (String resourcePath : resourceFiles) {
            String fileName = new File(resourcePath).getName();
            File targetFile = new File(backendRoot.toString(), fileName);

            if (!targetFile.exists()) {
                try (InputStream resourceStream = getClass().getResourceAsStream(resourcePath)) {
                    if (resourceStream == null) {
                        LOGGER.error("Resource " + resourcePath + " not found.");
                        continue;
                    }

                    Files.copy(resourceStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    LOGGER.error("Failed to copy " + fileName + ": " + e.getMessage());
                }
            }
        }

        try {
            stateFile.createNewFile();
            Files.writeString(stateFile.toPath(), "already-initialized=true");
        } catch (IOException e) {
            LOGGER.error("Failed to create state file: " + e.getMessage());
        }

        Path staticDir = serverRoot.resolve("static");

        if (!Files.exists(staticDir)) {
            try {
                Files.createDirectory(staticDir);
            } catch (IOException e) {
                LOGGER.error("Failed to create static directory: " + e.getMessage());
            }
        }
    }

    public boolean isActive() {
        return active;
    }

    // Parses the host information from a configuration file
    private void parseBindInfo() {
        try {
            File configFile = new File(backendRoot.toString(), "javaweb.xml");

            if (!configFile.exists()) {
                LOGGER.error("Config file not found: " + configFile.getAbsolutePath());
                return;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(configFile);
            doc.getDocumentElement().normalize();

            NodeList bindInformation = doc.getElementsByTagName("bind");

            if (bindInformation.getLength() > 0) {
                Element bindElement = (Element) bindInformation.item(0);
                String _host = bindElement.getElementsByTagName("host").item(0).getTextContent().trim();
                String _port = bindElement.getElementsByTagName("port").item(0).getTextContent().trim();
                host = _host;
                port = Integer.parseInt(_port);
                LOGGER.info("Using host " + host + "...");
                LOGGER.info("Using port " + port + "...");
            } else {
                LOGGER.warn("No bind information found in configuration! Using defaults...");
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing bind information: " + e.getMessage());
        }
    }

    // Gets the configured routes and applies them
    private void parseRoutes() {
        try {
            File configFile = new File(backendRoot.toString(), "javaweb.xml");

            if (!configFile.exists()) {
                LOGGER.error("Config file not found: " + configFile.getAbsolutePath());
                return;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(configFile);
            doc.getDocumentElement().normalize();

            NodeList routeNodes = doc.getElementsByTagName("route");

            for (int i = 0; i < routeNodes.getLength(); i++) {
                if (routeNodes.item(i) instanceof Element) {
                    Element routeElem = (Element) routeNodes.item(i);
                    String path = routeElem.getElementsByTagName("path").item(0).getTextContent().trim();
                    String publicFile = routeElem.getElementsByTagName("public-file").item(0).getTextContent().trim();
                    routes.add(new Route() {
                        @Override
                        public String getPath() {
                            return path;
                        }

                        @Override
                        public String getPublicFile() {
                            return publicFile;
                        }
                    });
                }
            }
            LOGGER.info("Loaded " + routes.size() + " routes from configuration.");
        } catch (Exception e) {
            LOGGER.error("Error parsing routes: " + e.getMessage());
        }
    }

    // Handles a request
    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                OutputStream out = clientSocket.getOutputStream()) {
            String requestLine = in.readLine();

            if (requestLine == null || requestLine.isEmpty()) {
                return;
            }

            String[] tokens = requestLine.split(" ");

            if (tokens.length < 2 || !tokens[0].equals("GET")) {
                sendErrorResponse(out, 405, "Method Not Allowed");
                return;
            }

            String requestPath = tokens[1].split("\\?")[0];

            for (Route route : routes) {
                if (requestPath.equals(route.getPath())) {
                    requestPath = "/" + route.getPublicFile();
                    break;
                }
            }

            Path staticDir = serverRoot.resolve("static");
            Path filePath = staticDir.resolve(requestPath.substring(1)).normalize();

            if (!filePath.startsWith(staticDir)) {
                sendErrorResponse(out, 403, "Forbidden");
                return;
            }

            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                sendErrorResponse(out, 404, "Not Found");
                return;
            }

            byte[] fileData = Files.readAllBytes(filePath);
            String contentType = Files.probeContentType(filePath);

            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            String responseHeader = "HTTP/1.1 200 OK\r\n" +
                    "Content-Length: " + fileData.length + "\r\n" +
                    "Content-Type: " + contentType + "\r\n" +
                    "\r\n";
            out.write(responseHeader.getBytes());
            out.write(fileData);
            out.flush();
        } catch (IOException e) {
            LOGGER.error("Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                LOGGER.error("Error closing client socket: " + e.getMessage());
            }
        }
    }

    // Sends an error response
    private void sendErrorResponse(OutputStream out, int statusCode, String message) throws IOException {
        String response = "HTTP/1.1 " + statusCode + " " + message + "\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + (message.length() + 50) + "\r\n" +
                "\r\n" +
                "<html><body><h1>" + statusCode + " " + message + "</h1></body></html>";
        out.write(response.getBytes());
        out.flush();
    }
}
