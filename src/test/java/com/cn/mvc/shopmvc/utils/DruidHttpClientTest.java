package com.cn.mvc.shopmvc.utils;

import com.sun.net.httpserver.HttpServer;
import org.junit.Test;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class DruidHttpClientTest {
    @Test
    public void sendsRpcAsPostAndClosesHttpConnection() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> request = new AtomicReference<>();
        AtomicReference<String> connection = new AtomicReference<>();
        server.createContext("/rpc", exchange -> {
            connection.set(exchange.getRequestHeaders().getFirst("Connection"));
            request.set(exchange.getRequestMethod() + " " + new String(
                    org.apache.calcite.avatica.AvaticaUtils.readFullyToBytes(exchange.getRequestBody()), StandardCharsets.UTF_8));
            byte[] body = "{\"response\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            DruidHttpClient client = new DruidHttpClient(new URL("http://127.0.0.1:" + server.getAddress().getPort() + "/rpc"));
            assertEquals("{\"response\":\"ok\"}", new String(client.send("{\"request\":1}".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
            assertEquals("POST {\"request\":1}", request.get());
            assertEquals("close", connection.get());
        } finally { server.stop(0); }
    }

    @Test
    public void preservesAvaticaErrorResponseForTheDriver() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/rpc", exchange -> {
            byte[] body = "{\"response\":\"error\",\"errorMessage\":\"Missing datasource\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            DruidHttpClient client = new DruidHttpClient(new URL("http://127.0.0.1:" + server.getAddress().getPort() + "/rpc"));
            assertTrue(new String(client.send(new byte[0]), StandardCharsets.UTF_8).contains("Missing datasource"));
        } finally { server.stop(0); }
    }
}
