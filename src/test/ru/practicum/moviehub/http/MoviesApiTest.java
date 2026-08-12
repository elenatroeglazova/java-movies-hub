package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080"; // !!! добавьте базовую часть URL
    private static MoviesServer server;
    private static HttpClient client;
    private static final MoviesStore movies = new MoviesStore();
    private static final Gson gson = new Gson();

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer(movies, 8080);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        movies.clear();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertEquals("[]", body, "Ожидается пустой JSON-массив");
    }

    @Test
    void getMovies_whenNotEmpty_returnsArrayOfMovies() throws IOException, InterruptedException {
        movies.add(new Movie("Крестный отец", 1972, 1));
        movies.add(new Movie("Дюна", 2021, 2));
        String jsonBody = gson.toJson(movies.getFirst());

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        client.send(req, HttpResponse.BodyHandlers.ofString(UTF_8));
        String jsonBody1 = gson.toJson(movies.get(1));

        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody1))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();
        client.send(req1, HttpResponse.BodyHandlers.ofString(UTF_8));

        String moviesJson = gson.toJson(movies);
        HttpRequest resReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(resReq, HttpResponse.BodyHandlers.ofString(UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertEquals(moviesJson, body, "Ожидается JSON-массив, содержащий список фильмов");
    }

    @Test
    void postMovies_whenNotErroneous_returnsMovieId() throws IOException, InterruptedException {
        Movie newMovie = new Movie("Крестный отец", 1972);
        String jsonBody = gson.toJson(newMovie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(UTF_8));

        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        Movie respMovie = gson.fromJson(body, Movie.class);

        assertEquals(3, respMovie.getId(), "ID фильма должно быть равно 1");
        assertEquals(newMovie, respMovie, "В ответе должны быть данные фильма из запроса");
    }

    @Test
    void postMovies_whenWrongContentType_returnsErrorResponse() throws IOException, InterruptedException {
        Movie newMovie = new Movie("Крестный отец", 1972);
        String jsonBody = gson.toJson(newMovie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(UTF_8));

        assertEquals(415, resp.statusCode(), "POST /movies должен вернуть 201");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        ErrorResponse errorResponse = gson.fromJson(body, ErrorResponse.class);

        assertEquals("Не поддерживаемый тип данных", errorResponse.getError(),
                "В ответе должно быть сообщение об ошибке 'Не поддерживаемый тип данных'");
    }

    @Test
    void postMovies_whenTooLongMovieTitle_returnsValidationErrorResponse() throws IOException, InterruptedException {
        Movie newMovie = new Movie("Борат: Передача искаженного культурного влияния на благославленную " +
                "в прошлом славную нацию Казахстана", 2006);
        String jsonBody = gson.toJson(newMovie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(UTF_8));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        ErrorResponse errorResponse = gson.fromJson(body, ErrorResponse.class);

        assertEquals("Ошибка валидации", errorResponse.getError(),
                "В ответе должно быть сообщение об ошибке 'Ошибка валидации'");
        assertEquals(new ArrayList<>(List.of("максимальная длина наименования - 100 знаков")),
                errorResponse.getDetails(), "Детализация должна содержать " +
                        "'максимальная длина наименования - 100 знаков'");
    }

    @Test
    void postMovies_whenEmptyMovieTitle_returnsValidationErrorResponse() throws IOException, InterruptedException {
        Movie newMovie = new Movie("", 2006);
        String jsonBody = gson.toJson(newMovie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(UTF_8));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        ErrorResponse errorResponse = gson.fromJson(body, ErrorResponse.class);

        assertEquals("Ошибка валидации", errorResponse.getError(),
                "В ответе должно быть сообщение об ошибке 'Ошибка валидации'");
        assertEquals(new ArrayList<>(List.of("название не должно быть пустым")),
                errorResponse.getDetails(), "Детализация должна содержать 'название не должно быть пустым'");
    }

    @Test
    void postMovies_whenYearAfter2026_returnsValidationErrorResponse() throws IOException, InterruptedException {
        Movie newMovie = new Movie("Звездные войны", 2028);
        String jsonBody = gson.toJson(newMovie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(UTF_8));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        ErrorResponse errorResponse = gson.fromJson(body, ErrorResponse.class);

        assertEquals("Ошибка валидации", errorResponse.getError(),
                "В ответе должно быть сообщение об ошибке 'Ошибка валидации'");
        assertEquals(new ArrayList<>(List.of("год должен быть между 1888 и 2026")),
                errorResponse.getDetails(), "Детализация должна содержать 'год должен быть между 1888 и 2026'");
    }

    @Test
    void postMovies_whenYearBefore1888_returnsValidationErrorResponse() throws IOException, InterruptedException {
        Movie newMovie = new Movie("Звездные войны", 1028);
        String jsonBody = gson.toJson(newMovie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(UTF_8));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        ErrorResponse errorResponse = gson.fromJson(body, ErrorResponse.class);

        assertEquals("Ошибка валидации", errorResponse.getError(),
                "В ответе должно быть сообщение об ошибке 'Ошибка валидации'");
        assertEquals(new ArrayList<>(List.of("год должен быть между 1888 и 2026")),
                errorResponse.getDetails(), "Детализация должна содержать 'год должен быть между 1888 и 2026'");
    }

    @Test
    void postMovies_whenIncorrectTitleAndYear_returnsBothValidationErrorsResponse() throws IOException, InterruptedException {
        Movie newMovie = new Movie("Борат: Передача искаженного культурного влияния на благославленную " +
                "в прошлом славную нацию Казахстана", 1028);
        String jsonBody = gson.toJson(newMovie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(UTF_8));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        ErrorResponse errorResponse = gson.fromJson(body, ErrorResponse.class);

        assertEquals("Ошибка валидации", errorResponse.getError(),
                "В ответе должно быть сообщение об ошибке 'Ошибка валидации'");
        assertEquals(new ArrayList<>(List.of("максимальная длина наименования - 100 знаков",
                        "год должен быть между 1888 и 2026")), errorResponse.getDetails(),
                "Детализация должна содержать 2 описания ошибки");
    }

    @Test
    void postMovies_whenMalformedJson_returnsJsonSyntaxErrorResponse() throws IOException, InterruptedException {
        String jsonBody = "{title=Звездные войны,year=2028}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(UTF_8));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        ErrorResponse errorResponse = gson.fromJson(body, ErrorResponse.class);

        assertEquals("Ошибка в синтаксисе JSON", errorResponse.getError(),
                "В ответе должно быть сообщение об ошибке 'Ошибка в синтаксисе JSON'");
    }
}