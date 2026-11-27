package com.cn.mvc.shopmvc.utils;

import org.apache.calcite.avatica.AvaticaUtils;
import org.apache.calcite.avatica.remote.AvaticaHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** HTTP transport for Avatica 1.12, whose default client owns an unclosed pool per JDBC connection. */
public class DruidHttpClient implements AvaticaHttpClient {
    private final URL url;

    public DruidHttpClient(URL url) {
        this.url = url;
    }

    @Override
    public byte[] send(byte[] request) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(10000);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Connection", "close");
            connection.setDoOutput(true);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(request);
            }
            int status = connection.getResponseCode();
            InputStream response = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (response == null) {
                throw new IOException("Druid Avatica returned HTTP " + status + " without a response body");
            }
            try (InputStream input = response) {
                // Preserve structured Avatica error bodies so the JDBC driver can report the SQL error.
                return AvaticaUtils.readFullyToBytes(input);
            }
        } catch (IOException e) {
            throw new RuntimeException("Druid Avatica HTTP request failed: " + url, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
