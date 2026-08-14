package ru.practicum.moviehub.http;

import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;
    private Movie newMovie;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        if (method.equalsIgnoreCase("GET")) {
            String path = ex.getRequestURI().getPath();
            String[] pathComponents = path.split("/");

            if (pathComponents.length == 3) {
                Optional<Integer> idOpt = getId(pathComponents[2]);

                if (idOpt.isEmpty()) {
                    sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный ID")));
                } else {
                    Movie movie = store.get(idOpt.get());
                    if (movie == null) {
                        sendJson(ex, 404, gson.toJson(new ErrorResponse("Фильм не найден")));
                    } else {
                        sendJson(ex, 200, gson.toJson(movie));
                    }
                }
            } else {
                String moviesJson = gson.toJson(store.values());
                sendJson(ex, 200, moviesJson);
            }
        } else if (method.equalsIgnoreCase("POST")) {
            String respHeader = ex.getRequestHeaders().getFirst("Content-Type");
            if (respHeader == null || !respHeader.equals("application/json; charset=UTF-8")) {
                sendJson(ex, 415, gson.toJson(new ErrorResponse("Не поддерживаемый тип данных")));
            }

            Type movieType = new TypeToken<Movie>() {
            }.getType();
            Optional<Movie> movieOpt = getBody(ex, movieType);

            if (movieOpt.isEmpty()) {
                sendJson(ex, 422, gson.toJson(new ErrorResponse("Ошибка в синтаксисе JSON")));
            } else {
                newMovie = movieOpt.get();
            }

            ErrorResponse errResp = checkValidationErrors();

            if (errResp.getDetails().isEmpty()) {
                store.put(store.size() + 1, newMovie);
                sendJson(ex, 201, gson.toJson(
                        Collections.singletonMap(store.lastEntry().getKey(), store.lastEntry().getValue())));
            } else {
                sendJson(ex, 422, gson.toJson(errResp));
            }
        } else if (method.equalsIgnoreCase("DELETE")) {
            String path = ex.getRequestURI().getPath();
            String[] pathComponents = path.split("/");
            Optional<Integer> idOpt = getId(pathComponents[2]);

            if (idOpt.isEmpty()) {
                sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный ID")));
            } else {
                Movie movie = store.remove(idOpt.get());
                if (movie == null) {
                    sendJson(ex, 404, gson.toJson(new ErrorResponse("Фильм не найден")));
                } else {
                    sendNoContent(ex);
                }
            }
        } else {
            ErrorResponse err = new ErrorResponse("Неподдерживаемый метод");
            sendJson(ex, 405, gson.toJson(err));
        }
    }

    private ErrorResponse checkValidationErrors() {
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
        return errResp;
    }
}
