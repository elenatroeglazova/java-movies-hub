package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CT_JSON = "application/json; charset=UTF-8";
    protected final Gson gson = new Gson();

    protected void sendJson(HttpExchange ex, int status, String json) throws IOException {
        ex.sendResponseHeaders(status, 0);

        try (OutputStream os = ex.getResponseBody()) {
            os.write(json.getBytes(UTF_8));
        }
        ex.close();
    }

    protected void sendNoContent(HttpExchange ex) throws java.io.IOException {
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(204, -1);
    }

    protected <T> Optional<T> getBody(HttpExchange ex, Type type) throws IOException {
        byte[] bytes = ex.getRequestBody().readAllBytes();
        if (bytes.length == 0) {
            return Optional.empty();
        }
        String body = new String(bytes, UTF_8);
        try {
            return Optional.of(gson.fromJson(body, type));
        } catch (JsonSyntaxException e) {
            return Optional.empty();
        }
    }

    protected Optional<Integer> getNumber(String numValue) {
        try {
            Integer id = Integer.parseInt(numValue);
            return Optional.of(id);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}