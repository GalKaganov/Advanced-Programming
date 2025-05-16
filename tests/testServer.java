package test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.*;

public class testServer {

    private static class TestServlet implements Servlet {
        private final String response;
        private volatile boolean closed = false;

        public TestServlet(String response) {
            this.response = response;
        }

        @Override
        public void handle(RequestParser.RequestInfo ri, OutputStream toClient) throws IOException {
            // Build a simple HTTP response with the content we want to send
            String httpResponse = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Content-Length: " + response.getBytes().length + "\r\n" +
                    "\r\n" +
                    response;

            toClient.write(httpResponse.getBytes());
            toClient.flush();
        }

        @Override
        public void close() throws IOException {
            this.closed = true;
        }

        public boolean isClosed() {
            return closed;
        }
    }

    private static class EchoServlet implements Servlet {
        private volatile boolean closed = false;

        @Override
        public void handle(RequestParser.RequestInfo ri, OutputStream toClient) throws IOException {
            // Echo back the parameters and URI
            StringBuilder responseContent = new StringBuilder();
            responseContent.append("HTTP Command: ").append(ri.getHttpCommand()).append("\n");
            responseContent.append("URI: ").append(ri.getUri()).append("\n");

            responseContent.append("URI Segments: ");
            for (String segment : ri.getUriSegments()) {
                responseContent.append(segment).append(", ");
            }
            responseContent.append("\n");

            responseContent.append("Parameters:\n");
            for (String key : ri.getParameters().keySet()) {
                responseContent.append("  ").append(key).append("=").append(ri.getParameters().get(key)).append("\n");
            }

            String content = responseContent.toString();
            String httpResponse = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Content-Length: " + content.getBytes().length + "\r\n" +
                    "\r\n" +
                    content;

            toClient.write(httpResponse.getBytes());
            toClient.flush();
        }

        @Override
        public void close() throws IOException {
            this.closed = true;
        }

        public boolean isClosed() {
            return closed;
        }
    }

    private static String sendRequest(String host, int port, String request) throws IOException {
        try (Socket socket = new Socket(host, port);
             OutputStream out = socket.getOutputStream();
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Send request
            out.write(request.getBytes(StandardCharsets.UTF_8));
            out.flush();

            // Read response
            StringBuilder response = new StringBuilder();
            String line;

            // Skip headers
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                // Reading headers
            }

            // Read content
            while (in.ready() && (line = in.readLine()) != null) {
                response.append(line).append("\n");
            }

            return response.toString();
        }
    }

    private static boolean testThreadPool(int expectedNumber) throws InterruptedException {
        // Let's wait some time to ensure thread pool stabilizes
        Thread.sleep(500);

        // Get the current thread count
        int activeThreads = Thread.activeCount();

        // We expect main thread + server thread + thread pool threads
        System.out.println("Active threads: " + activeThreads);

        // This is a simplification, but should give us a general idea
        // In reality, you'd need more sophisticated thread monitoring
        return activeThreads >= expectedNumber && activeThreads <= expectedNumber + 3;
    }

    public static void main(String[] args) {
        int port = 8080;
        int nThreads = 5;
        HTTPServer server = null;

        try {
            System.out.println("Testing HTTP server...");

            // Create server
            server = new MyHTTPServer(port, nThreads);

            // Create test servlets
            TestServlet getServlet = new TestServlet("Hello from GET servlet!");
            EchoServlet echoServlet = new EchoServlet();
            TestServlet postServlet = new TestServlet("Hello from POST servlet!");
            TestServlet deleteServlet = new TestServlet("Hello from DELETE servlet!");

            // Register servlets
            server.addServlet("GET", "/api", getServlet);
            server.addServlet("GET", "/echo", echoServlet);
            server.addServlet("POST", "/api", postServlet);
            server.addServlet("DELETE", "/api", deleteServlet);

            // Start server
            server.start();

            System.out.println("Server started on port " + port);

            // Test if thread pool is properly configured
            if (testThreadPool(nThreads + 2)) { // +2 for main thread and server thread
                System.out.println("✓ Thread pool size verified");
            } else {
                System.out.println("✗ Thread pool size test failed");
            }

            // Test GET request
            String getRequest = "GET /api/resource?id=123&name=test HTTP/1.1\r\n" +
                    "Host: localhost\r\n" +
                    "\r\n";

            String getResponse = sendRequest("localhost", port, getRequest);
            System.out.println("GET response: " + getResponse);
            if (getResponse.contains("Hello from GET servlet!")) {
                System.out.println("✓ GET request passed");
            } else {
                System.out.println("✗ GET request failed");
            }

            // Test POST request
            String postRequest = "POST /api/resource HTTP/1.1\r\n" +
                    "Host: localhost\r\n" +
                    "Content-Length: 11\r\n" +
                    "\r\n" +
                    "Hello World";

            String postResponse = sendRequest("localhost", port, postRequest);
            System.out.println("POST response: " + postResponse);
            if (postResponse.contains("Hello from POST servlet!")) {
                System.out.println("✓ POST request passed");
            } else {
                System.out.println("✗ POST request failed");
            }

            // Test DELETE request
            String deleteRequest = "DELETE /api/resource HTTP/1.1\r\n" +
                    "Host: localhost\r\n" +
                    "\r\n";

            String deleteResponse = sendRequest("localhost", port, deleteRequest);
            System.out.println("DELETE response: " + deleteResponse);
            if (deleteResponse.contains("Hello from DELETE servlet!")) {
                System.out.println("✓ DELETE request passed");
            } else {
                System.out.println("✗ DELETE request failed");
            }

            // Test URI prefix matching
            String echoRequest = "GET /echo/test?param1=value1&param2=value2 HTTP/1.1\r\n" +
                    "Host: localhost\r\n" +
                    "\r\n";

            String echoResponse = sendRequest("localhost", port, echoRequest);
            System.out.println("Echo response: " + echoResponse);
            if (echoResponse.contains("URI: /echo/test?param1=value1&param2=value2") &&
                    echoResponse.contains("param1=value1") &&
                    echoResponse.contains("param2=value2")) {
                System.out.println("✓ URI prefix matching passed");
            } else {
                System.out.println("✗ URI prefix matching failed");
            }

            // Test servlet removal
            server.removeServlet("GET", "/api");

            try {
                String removedResponse = sendRequest("localhost", port, getRequest);
                if (!removedResponse.contains("Hello from GET servlet!")) {
                    System.out.println("✓ Servlet removal passed");
                } else {
                    System.out.println("✗ Servlet removal failed");
                }
            } catch (IOException e) {
                // Expected 404 response might cause connection to close
                System.out.println("✓ Servlet removal passed (connection closed)");
            }

            // Wait for a bit to ensure all tests have run
            Thread.sleep(1000);

            System.out.println("All tests completed!");

        } catch (Exception e) {
            System.err.println("Test failed with exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Clean up
            if (server != null) {
                server.close();
                System.out.println("Server closed");

                // Wait to ensure threads have time to shut down
                try {
                    Thread.sleep(2000);
                    int finalThreadCount = Thread.activeCount();
                    System.out.println("Final thread count: " + finalThreadCount);
                    if (finalThreadCount <= 2) { // Main thread + possibly one more
                        System.out.println("✓ Thread cleanup successful");
                    } else {
                        System.out.println("✗ Thread cleanup failed, some threads might still be running");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * A simple HTTP server implementation that manages and dispatches HTTP requests
     * to the appropriate servlets based on the request type and URI.
     */
    public static class MyHTTPServer extends Thread implements HTTPServer {

        /** Concurrent map to manage servlets for GET,POST,DELETE requests. */
        private ConcurrentHashMap<String, Servlet> getServlets = new ConcurrentHashMap<>();
        private ConcurrentHashMap<String, Servlet> postServlets = new ConcurrentHashMap<>();
        private ConcurrentHashMap<String, Servlet> deleteServlets = new ConcurrentHashMap<>();

        /** Thread pool to handle multiple client connections concurrently. */
        private ExecutorService requestHandlerPool;

        /** Socket used to accept client connections. */
        private ServerSocket serverSocket;

        /** Flag to indicate if the server should stop accepting requests. */
        private volatile boolean isServerStopped = false;

        /** Port number on which the server listens for incoming connections. */
        private final int port;

        /** Number of threads in the thread pool for handling requests. */
        private final int threadCount;

        /**
         * Constructs a new HTTP server instance with the specified port and thread count.
         *
         * @param port The port number for the server to listen on.
         * @param threadCount The number of threads in the thread pool.
         */
        public MyHTTPServer(int port, int threadCount) {
            // Initialize the thread pool with a fixed number of threads
            requestHandlerPool = Executors.newFixedThreadPool(threadCount);
            this.port = port;
            this.threadCount = threadCount;
        }

        /**
         * Registers a servlet to handle requests for a specific HTTP command and URI.
         *
         * @param httpCommand The HTTP command (e.g., GET, POST, DELETE) for which the servlet will handle requests.
         * @param uri The URI that the servlet will handle.
         * @param servlet The servlet instance to handle the requests.
         */
        public void addServlet(String httpCommand, String uri, Servlet servlet) {
            if (uri == null || servlet == null) {
                return;
            }

            httpCommand = httpCommand.toUpperCase();

            switch (httpCommand) {
                case "GET":
                    getServlets.put(uri, servlet);
                    break;
                case "POST":
                    postServlets.put(uri, servlet);
                    break;
                case "DELETE":
                    deleteServlets.put(uri, servlet);
                    break;
            }
        }

        /**
         * Removes a servlet that handles requests for a specific HTTP command and URI.
         *
         * @param httpCommand The HTTP command (e.g., GET, POST, DELETE) for which the servlet was handling requests.
         * @param uri The URI that the servlet was handling.
         */
        public void removeServlet(String httpCommand, String uri) {
            if (uri == null) {
                return;
            }

            httpCommand = httpCommand.toUpperCase();

            switch (httpCommand) {
                case "GET":
                    getServlets.remove(uri);
                    break;
                case "POST":
                    postServlets.remove(uri);
                    break;
                case "DELETE":
                    deleteServlets.remove(uri);
                    break;
            }
        }

        /**
         * Starts the HTTP server to listen for and handle client connections.
         */
        public void run() {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                this.serverSocket = serverSocket;
                serverSocket.setSoTimeout(1000); // Set timeout for socket accept operations

                while (!isServerStopped) {
                    try {
                        // Accept a new client connection
                        Socket clientSocket = serverSocket.accept();

                        // Handle the client request in a separate thread
                        requestHandlerPool.submit(() -> {
                            try {
                                Thread.sleep(125); // Delay to ensure proper request reception
                                BufferedReader requestReader = createBufferedReader(clientSocket);

                                // Parse the incoming request
                                RequestParser.RequestInfo requestInfo = RequestParser.parseRequest(requestReader);
                                ConcurrentHashMap<String, Servlet> servletMap;

                                if (requestInfo != null) {
                                    switch (requestInfo.getHttpCommand()) {
                                        case "GET":
                                            servletMap = getServlets;
                                            break;
                                        case "POST":
                                            servletMap = postServlets;
                                            break;
                                        case "DELETE":
                                            servletMap = deleteServlets;
                                            break;
                                        default:
                                            throw new IllegalArgumentException("Unsupported HTTP command: " + requestInfo.getHttpCommand());
                                    }

                                    // Find the best matching servlet based on the longest URI match
                                    String bestMatchUri = "";
                                    Servlet matchingServlet = null;
                                    for (Map.Entry<String, Servlet> entry : servletMap.entrySet()) {
                                        if (requestInfo.getUri().startsWith(entry.getKey()) && entry.getKey().length() > bestMatchUri.length()) {
                                            bestMatchUri = entry.getKey();
                                            matchingServlet = entry.getValue();
                                        }
                                    }

                                    // Handle the request using the matching servlet
                                    if (matchingServlet != null) {
                                        matchingServlet.handle(requestInfo, clientSocket.getOutputStream());
                                    }
                                }
                                requestReader.close();
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                            } finally {
                                // Close the client connection
                                try {
                                    clientSocket.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } catch (IOException e) {
                        // Handle socket accept timeout exception
                        if (isServerStopped) {
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Creates a BufferedReader to read from the client socket.
         *
         * @param clientSocket The client socket.
         * @return A BufferedReader to read from the socket.
         * @throws IOException If an I/O error occurs.
         */
        private static BufferedReader createBufferedReader(Socket clientSocket) throws IOException {
            InputStream inputStream = clientSocket.getInputStream();
            int availableBytes = inputStream.available();
            byte[] buffer = new byte[availableBytes];
            int bytesRead = inputStream.read(buffer, 0, availableBytes);

            return new BufferedReader(
                    new InputStreamReader(
                            new ByteArrayInputStream(buffer, 0, bytesRead)
                    )
            );
        }

        /**
         * Starts the HTTP server to begin accepting and handling requests.
         */
        public void start() {
            isServerStopped = false;
            super.start();
        }

        /**
         * Stops the HTTP server and shuts down the thread pool.
         */
        public void close() {
            isServerStopped = true;
            requestHandlerPool.shutdownNow();
        }

        /**
         * Gets the thread pool used by the server for handling client requests.
         *
         * @return The thread pool.
         */
        public Object getThreadPool() {
            return requestHandlerPool;
        }
    }
}