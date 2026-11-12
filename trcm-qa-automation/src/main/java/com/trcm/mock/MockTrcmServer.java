package com.trcm.mock;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * MOCK SERVER FOR LEARNING ONLY.
 *
 * Uses the JDK's built-in HttpServer (no extra dependency) to stand in for three things:
 *
 *   1. The TRCM web app             ->  http://localhost:8089/app/batch-review.html
 *   2. Azure API Management (APIM)  ->  http://localhost:8089/apim/...
 *   3. The backend Coding Agent API ->  http://localhost:8089/service/...
 *
 * Request flow it imitates (same as the real APIM setup in APIM_Integration_Testing_1.xlsx):
 *
 *   Client --(Ocp-Apim-Subscription-Key)--> APIM --(X-API-Key from "Named Value")--> Service
 *
 * Run standalone:  mvn compile exec:java
 */
public final class MockTrcmServer {

    public static final String VALID_SUBSCRIPTION_KEY = "fake-apim-subscription-key-123";
    public static final String VALID_SERVICE_API_KEY = "fake-service-api-key-456";

    private static HttpServer server;
    /** Simulates the APIM "Named Value" that stores the backend API key (TC-06 changes it). */
    private static volatile String apimNamedValueServiceKey = VALID_SERVICE_API_KEY;

    private MockTrcmServer() {
    }

    public static synchronized boolean isRunning() {
        return server != null;
    }

    public static synchronized void start(int port) throws IOException {
        if (server != null) {
            return; // idempotent: several test classes may call start()
        }
        server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        server.setExecutor(Executors.newFixedThreadPool(8));

        server.createContext("/app/", MockTrcmServer::serveStatic);
        server.createContext("/service/health", ex -> send(ex, 200, "{\"status\": \"healthy\"}"));
        server.createContext("/apim/health", ex -> send(ex, 200, "{\"status\": \"healthy\"}"));
        server.createContext("/service/coding-agent/chat", MockTrcmServer::serviceChat);
        server.createContext("/apim/coding-agent/chat", MockTrcmServer::apimChat);
        server.createContext("/admin/named-value", MockTrcmServer::adminNamedValue);
        server.createContext("/", ex -> {
            ex.getResponseHeaders().add("Location", "/app/batch-review.html");
            ex.sendResponseHeaders(302, -1);
            ex.close();
        });
        server.start();
        System.out.println("[MockTrcmServer] started on http://localhost:" + port + "/app/batch-review.html");
    }

    public static synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            apimNamedValueServiceKey = VALID_SERVICE_API_KEY;
            System.out.println("[MockTrcmServer] stopped");
        }
    }

    // ----------------------------------------------------------------- handlers

    /** Fake APIM gateway: validates the subscription key, then forwards with X-API-Key. */
    private static void apimChat(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, "{\"error\": \"Method not allowed\"}");
            return;
        }
        String subKey = ex.getRequestHeaders().getFirst("Ocp-Apim-Subscription-Key");
        if (subKey == null || subKey.isBlank()) {
            send(ex, 401, "{\"statusCode\": 401, \"message\": \"Access denied due to missing subscription key.\"}");
            return;
        }
        if (!VALID_SUBSCRIPTION_KEY.equals(subKey)) {
            send(ex, 401, "{\"statusCode\": 401, \"message\": \"Access denied due to invalid subscription key.\"}");
            return;
        }
        // APIM policy injects the backend key from its Named Value, then calls the service.
        String body = readBody(ex);
        respondAsService(ex, apimNamedValueServiceKey, body);
    }

    /** Fake backend service: protected on its own by X-API-Key (defense in depth). */
    private static void serviceChat(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, "{\"error\": \"Method not allowed\"}");
            return;
        }
        String apiKey = ex.getRequestHeaders().getFirst("X-API-Key");
        respondAsService(ex, apiKey, readBody(ex));
    }

    private static void respondAsService(HttpExchange ex, String apiKey, String body) throws IOException {
        if (apiKey == null || apiKey.isBlank()) {
            send(ex, 401, "{\"detail\": \"Missing API key\"}");
            return;
        }
        if (!VALID_SERVICE_API_KEY.equals(apiKey)) {
            send(ex, 403, "{\"detail\": \"Invalid API key\"}");
            return;
        }
        String reply = body.toLowerCase().contains("rejected")
                ? "3 claims were rejected in the last batch."
                : "I can help with claim batch review questions.";
        send(ex, 200, "{\"reply\": \"" + reply + "\", \"source\": \"coding-agent-mock\"}");
    }

    /** Test hook that imitates editing the APIM Named Value in the Azure portal (TC-06). */
    private static void adminNamedValue(HttpExchange ex) throws IOException {
        String query = ex.getRequestURI().getQuery();
        if (query != null && query.startsWith("value=")) {
            apimNamedValueServiceKey = query.substring("value=".length());
        } else {
            apimNamedValueServiceKey = VALID_SERVICE_API_KEY; // no value = reset
        }
        send(ex, 200, "{\"namedValueUpdated\": true}");
    }

    private static void serveStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath().substring("/app/".length());
        if (path.isEmpty() || path.contains("..")) {
            path = "batch-review.html";
        }
        try (InputStream in = openResource("mockapp/" + path)) {
            if (in == null) {
                send(ex, 404, "{\"error\": \"Not found\"}");
                return;
            }
            byte[] bytes = in.readAllBytes();
            ex.getResponseHeaders().add("Content-Type", contentType(path));
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = ex.getResponseBody()) {
                out.write(bytes);
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private static InputStream openResource(String name) {
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        InputStream in = context != null ? context.getResourceAsStream(name) : null;
        if (in == null) {
            in = ClassLoader.getSystemResourceAsStream(name);
        }
        if (in == null) {
            in = MockTrcmServer.class.getResourceAsStream("/" + name);
        }
        return in;
    }

    private static String contentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (path.endsWith(".css")) return "text/css; charset=utf-8";
        return "application/octet-stream";
    }

    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void send(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8089;
        start(port);
        System.out.println("Press Ctrl+C to stop.");
    }
}
