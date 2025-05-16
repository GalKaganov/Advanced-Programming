package test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import test.RequestParser.RequestInfo;

public class MyHTTPServer extends Thread implements HTTPServer {
    private final int port;
    private final ExecutorService threadPool;
    private ServerSocket serverSocket;
    private volatile boolean running;

    // Thread-safe maps to store servlets for different HTTP methods
    private final Map<String, Servlet> getServlets = new ConcurrentHashMap<>();
    private final Map<String, Servlet> postServlets = new ConcurrentHashMap<>();
    private final Map<String, Servlet> deleteServlets = new ConcurrentHashMap<>();

    public MyHTTPServer(int port, int nThreads) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(nThreads);
        this.running = false;
    }

    @Override
    public void addServlet(String httpCommand, String uri, Servlet s) {
        switch (httpCommand.toUpperCase()) {
            case "GET":
                getServlets.put(uri, s);
                break;
            case "POST":
                postServlets.put(uri, s);
                break;
            case "DELETE":
                deleteServlets.put(uri, s);
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP command: " + httpCommand);
        }
    }

    @Override
    public void removeServlet(String httpCommand, String uri) {
        switch (httpCommand.toUpperCase()) {
            case "GET":
                getServlets.remove(uri);
                break;
            case "POST":
                postServlets.remove(uri);
                break;
            case "DELETE":
                deleteServlets.remove(uri);
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP command: " + httpCommand);
        }
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(1000); // Wait for client for 1 second
            running = true;

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    // Handle client in the thread pool
                    threadPool.execute(() -> handleClient(clientSocket));
                } catch (SocketTimeoutException e) {
                    // This is expected - just retry
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error starting server on port " + port + ": " + e.getMessage());
        } finally {
            closeServerSocket();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                OutputStream writer = clientSocket.getOutputStream()
        ) {
            // Parse the request
            RequestInfo requestInfo = RequestParser.parseRequest(reader);
            if (requestInfo == null) {
                return;
            }

            // Get the appropriate servlet based on the HTTP command and URI
            Servlet servlet = findServlet(requestInfo.getHttpCommand(), requestInfo.getUri());

            if (servlet != null) {
                // Handle the request using the found servlet
                servlet.handle(requestInfo, writer);
            } else {
                // No servlet found - send 404 response
                String response = "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "Content-Length: 9\r\n" +
                        "\r\n" +
                        "Not Found";
                writer.write(response.getBytes());
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private Servlet findServlet(String httpCommand, String uri) {
        Map<String, Servlet> servletsMap;

        // Select the appropriate map based on the HTTP command
        switch (httpCommand.toUpperCase()) {
            case "GET":
                servletsMap = getServlets;
                break;
            case "POST":
                servletsMap = postServlets;
                break;
            case "DELETE":
                servletsMap = deleteServlets;
                break;
            default:
                return null;
        }

        // Find the servlet with the longest matching prefix
        String longestMatch = "";
        Servlet matchingServlet = null;

        for (Map.Entry<String, Servlet> entry : servletsMap.entrySet()) {
            String registeredUri = entry.getKey();

            // Check if the URI starts with the registered URI and it's longer than the current match
            if (uri.startsWith(registeredUri) && registeredUri.length() > longestMatch.length()) {
                longestMatch = registeredUri;
                matchingServlet = entry.getValue();
            }
        }

        return matchingServlet;
    }

    private void closeServerSocket() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing server socket: " + e.getMessage());
            }
        }
    }

    @Override
    public void close() {
        running = false;

        // Shutdown the thread pool
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(2, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Close the server socket
        closeServerSocket();
    }
}
