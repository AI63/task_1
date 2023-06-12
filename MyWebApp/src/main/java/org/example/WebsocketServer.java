package org.example;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class WebsocketServer extends WebSocketServer {

    private static final int HTTP_PORT = 8080;
    private static final int WS_PORT = 8887;
    private final Map<InetSocketAddress, WebSocket> clients = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private BigInteger lastNumber = null;

    public WebsocketServer() {
        super(new InetSocketAddress(WS_PORT));
        HttpServer httpServer;
        try {
            httpServer = HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);
            httpServer.createContext("/getNumber", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    InetSocketAddress remoteAddress = exchange.getRemoteAddress();
                    if (clients.containsKey(remoteAddress)) {
                        exchange.sendResponseHeaders(403, 0);
                    } else {
                        BigInteger number = generateUniqueNumber();
                        byte[] response = mapper.writeValueAsBytes(number);
                        exchange.sendResponseHeaders(200, response.length);
                        exchange.getResponseBody().write(response);
                        exchange.getResponseBody().close();
                    }
                }
            });
            httpServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        clients.put(conn.getRemoteSocketAddress(), conn);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        clients.remove(conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // do nothing
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {

    }

    private synchronized BigInteger generateUniqueNumber() {
        if (lastNumber == null) {
            lastNumber = BigInteger.valueOf(System.currentTimeMillis());
        } else {
            lastNumber = lastNumber.add(BigInteger.ONE);
        }
        return lastNumber;
    }

    public static void main(String[] args) {
        new WebsocketServer().start();
    }
}
