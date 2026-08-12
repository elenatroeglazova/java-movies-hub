package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.serializeNulls();
        Gson gson = gsonBuilder.create();
        String method = ex.getRequestMethod();
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        if (method.equalsIgnoreCase("GET")) {
            String moviesJson = gson.toJson(store);
            sendJson(ex, 200, moviesJson);
        } else if (method.equalsIgnoreCase("POST")) {
            String respHeader = ex.getRequestHeaders().getFirst("Content-Type");
            if (respHeader == null || !respHeader.equals("application/json; charset=UTF-8")) {
                sendJson(ex, 415, gson.toJson(new ErrorResponse("Не поддерживаемый тип данных")));
            }

            InputStream inputStream = ex.getRequestBody();
            String body = new String(inputStream.readAllBytes(), UTF_8);
            Movie newMovie;
            try {
                newMovie = gson.fromJson(body, Movie.class);
            } catch (JsonSyntaxException e) {
                sendJson(ex, 422, gson.toJson(new ErrorResponse("Ошибка в синтаксисе JSON")));
                return;
            }

            ErrorResponse errResp = new ErrorResponse("Ошибка валидации");
            String movieTitle = newMovie.getTitle();
            int movieYear = newMovie.getYear();

            if (movieTitle.length() > 100) {
                errResp.getDetails().add("максимальная длина наименования - 100 знаков");
            } else if (movieTitle.isBlank()) {
                errResp.getDetails().add("название не должно быть пустым");
            }

            if (movieYear > 2026 || movieYear < 1888) {
                errResp.getDetails().add("год должен быть между 1888 и 2026");
            }

            if (errResp.getDetails().isEmpty()) {
                newMovie.setId();
                store.add(newMovie);
                sendJson(ex, 201, gson.toJson(newMovie));
            } else {
                sendJson(ex, 422, gson.toJson(errResp));
            }
        }
    }
}
