package com.cn.mvc.shopmvc.utils;

import org.junit.Test;
import redis.clients.jedis.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class RedisUtilTest {
    @Test(timeout = 10000)
    public void commandsReturnPoolResourcesAndKeepConcurrentRepliesSeparate() throws Exception {
        ServerSocket server = new ServerSocket(0, 20, InetAddress.getByName("127.0.0.1"));
        List<Socket> sockets = new CopyOnWriteArrayList<>();
        Thread acceptor = new Thread(() -> {
            try {
                while (!server.isClosed()) {
                    Socket socket = server.accept();
                    sockets.add(socket);
                    Thread handler = new Thread(() -> serve(socket));
                    handler.setDaemon(true);
                    handler.start();
                }
            } catch (IOException ignored) { }
        });
        acceptor.setDaemon(true);
        acceptor.start();
        JedisPool previous = RedisUtil.jedisPool;
        JedisPool pool = new JedisPool(new JedisPoolConfig(), "127.0.0.1", server.getLocalPort(), 1500);
        RedisUtil.jedisPool = pool;
        ExecutorService workers = Executors.newFixedThreadPool(4);
        try {
            RedisUtil util = RedisUtil.build("127.0.0.1", server.getLocalPort());
            assertEquals("uv-date", util.hGet("uv", "date"));
            assertEquals("each command must return its connection", 0, pool.getNumActive());
            List<Future<String>> replies = new ArrayList<>();
            for (int i = 0; i < 40; i++) {
                final String key = "metric" + i;
                replies.add(workers.submit(() -> (String) util.hGet(key, "date")));
            }
            for (int i = 0; i < replies.size(); i++) assertEquals("metric" + i + "-date", replies.get(i).get());
            assertEquals(0, pool.getNumActive());
        } finally {
            workers.shutdownNow();
            pool.close();
            RedisUtil.jedisPool = previous;
            server.close();
            for (Socket socket : sockets) socket.close();
        }
    }

    private static void serve(Socket socket) {
        try (Socket owned = socket) {
            BufferedReader input = new BufferedReader(new InputStreamReader(owned.getInputStream(), StandardCharsets.UTF_8));
            OutputStream output = owned.getOutputStream();
            String header;
            while ((header = input.readLine()) != null) {
                int count = Integer.parseInt(header.substring(1));
                List<String> args = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    int length = Integer.parseInt(input.readLine().substring(1));
                    char[] value = new char[length];
                    int read = 0;
                    while (read < length) read += input.read(value, read, length - read);
                    input.readLine();
                    args.add(new String(value));
                }
                String response;
                if ("HGET".equalsIgnoreCase(args.get(0))) {
                    String value = args.get(1) + "-" + args.get(2);
                    response = "$" + value.length() + "\r\n" + value + "\r\n";
                } else response = "+PONG\r\n";
                output.write(response.getBytes(StandardCharsets.UTF_8));
                output.flush();
            }
        } catch (IOException ignored) { }
    }
}
