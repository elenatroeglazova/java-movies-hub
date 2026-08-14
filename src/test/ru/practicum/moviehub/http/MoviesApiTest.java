package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.LinkedHashSet;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

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
        LinkedHashSet<Movie> expected = new LinkedHashSet<>();
        Movie movie1 = new Movie("Крестный отец", 1972);
        Movie movie2 = new Movie("Дюна", 2021);
        movies.put(1, movie1);
        movies.put(2, movie2);

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
        expected.add(movie1);
        expected.add(movie2);
        String expectedJson = gson.toJson(expected);
        assertEquals(expectedJson, body, "Ожидается JSON-массив, содержащий список фильмов");
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

        JsonElement jsonElement = JsonParser.parseString(resp.body().trim());
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        Movie respMovie = gson.fromJson(jsonObject.get("1"), Movie.class);

        assertEquals("[1]", jsonObject.keySet().toString(), "ID фильма должно быть равно 1");
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

        assertEquals(415, resp.statusCode(), "POST /movies должен вернуть 415");

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

    @Test
    void getMovieById_whenNotEmpty_returnsMovieById() throws IOException, InterruptedException {
        Movie movie1 = new Movie("Крестный отец", 1972);
        Movie movie2 = new Movie("Дюна", 2021);
        movies.put(1, movie1);
        movies.put(2, movie2);

        HttpRequest resReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(resReq, HttpResponse.BodyHandlers.ofString(UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies/1 должен вернуть 200");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        String expectedJson = gson.toJson(movie1);
        assertEquals(expectedJson, body, "Ожидается JSON объект фильма с Id 1");
    }

    @Test
    void getMovieById_whenNotFound_returnsNotFoundError() throws IOException, InterruptedException {
        Movie movie1 = new Movie("Крестный отец", 1972);
        Movie movie2 = new Movie("Дюна", 2021);
        movies.put(1, movie1);
        movies.put(2, movie2);

        HttpRequest resReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/3"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(resReq, HttpResponse.BodyHandlers.ofString(UTF_8));

        assertEquals(404, resp.statusCode(), "GET /movies/3 должен вернуть 404");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        ErrorResponse errorResponse = gson.fromJson(body, ErrorResponse.class);

        assertEquals("Фильм не найден", errorResponse.getError(),
                "В ответе должно быть сообщение об ошибке 'Фильм не найден'");
    }

    @Test
    void getMovieById_whenNotANumber_returnsBadRequestError() throws IOException, InterruptedException {
        Movie movie1 = new Movie("Крестный отец", 1972);
        Movie movie2 = new Movie("Дюна", 2021);
        movies.put(1, movie1);
        movies.put(2, movie2);

        HttpRequest resReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/i"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(resReq, HttpResponse.BodyHandlers.ofString(UTF_8));

        assertEquals(400, resp.statusCode(), "GET /movies/i должен вернуть 400");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        ErrorResponse errorResponse = gson.fromJson(body, ErrorResponse.class);

        assertEquals("Некорректный ID", errorResponse.getError(),
                "В ответе должно быть сообщение об ошибке 'Некорректный ID'");
    }

    @Test
    void deleteMovieById_whenNotEmpty_deletesMovieById() throws IOException, InterruptedException {
        Movie movie1 = new Movie("Крестный отец", 1972);
        Movie movie2 = new Movie("Дюна", 2021);
        movies.put(1, movie1);
        movies.put(2, movie2);

        HttpRequest resReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(resReq, HttpResponse.BodyHandlers.ofString(UTF_8));

        assertEquals(204, resp.statusCode(), "DELETE /movies/1 должен вернуть 204");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();

        assertTrue(body.isBlank(), "Тело ответа должно быть пустым");
        assertNull(movies.get(1), "Фильм под id 1 должен быть удален из хранилища данных");
        assertEquals(1, movies.size(), "Должен сохраниться только один фильм");
        assertEquals(movie2, movies.get(2), "Остаться должен фильм с id 2");
    }

    @Test
    void deleteMovieById_whenNotFound_returnsNotFoundError() throws IOException, InterruptedException {
        Movie movie1 = new Movie("Крестный отец", 1972);
        Movie movie2 = new Movie("Дюна", 2021);
        movies.put(1, movie1);
        movies.put(2, movie2);

        HttpRequest resReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/3"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(resReq, HttpResponse.BodyHandlers.ofString(UTF_8));

        assertEquals(404, resp.statusCode(), "DELETE /movies/3 должен вернуть 404");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        ErrorResponse errorResponse = gson.fromJson(body, ErrorResponse.class);

        assertEquals("Фильм не найден", errorResponse.getError(),
                "В ответе должно быть сообщение об ошибке 'Фильм не найден'");
    }

    @Test
    void deleteMovieById_whenNotANumber_returnsBadRequestError() throws IOException, InterruptedException {
        Movie movie1 = new Movie("Крестный отец", 1972);
        Movie movie2 = new Movie("Дюна", 2021);
        movies.put(1, movie1);
        movies.put(2, movie2);

        HttpRequest resReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/i"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(resReq, HttpResponse.BodyHandlers.ofString(UTF_8));

        assertEquals(400, resp.statusCode(), "DELETE /movies/i должен вернуть 400");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        ErrorResponse errorResponse = gson.fromJson(body, ErrorResponse.class);

        assertEquals("Некорректный ID", errorResponse.getError(),
                "В ответе должно быть сообщение об ошибке 'Некорректный ID'");
    }

    @Test
    void getMoviesByYear_whenEmpty_returnsEmptyArray() throws Exception {
        Movie movie1 = new Movie("Оппенгеймер", 2023);
        Movie movie2 = new Movie("Барби", 2023);
        Movie movie3 = new Movie("Дюна: Часть вторая", 2023);
        movies.put(1, movie1);
        movies.put(2, movie2);
        movies.put(3, movie3);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2021"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies?year=2021 должен вернуть 200");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertEquals("[]", body, "Ожидается пустой JSON-массив");
    }

    @Test
    void getMoviesByYear_whenNotEmpty_returnsArrayOfMovies() throws IOException, InterruptedException {
        LinkedHashSet<Movie> expected = new LinkedHashSet<>();
        Movie movie1 = new Movie("Оппенгеймер", 2023);
        Movie movie2 = new Movie("Барби", 2023);
        Movie movie3 = new Movie("Дюна: Часть вторая", 2023);
        movies.put(1, movie1);
        movies.put(2, movie2);
        movies.put(3, movie3);

        HttpRequest resReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2023"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(resReq, HttpResponse.BodyHandlers.ofString(UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies?year=2023 должен вернуть 200");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        expected.add(movie1);
        expected.add(movie2);
        expected.add(movie3);
        String expectedJson = gson.toJson(expected);
        assertEquals(expectedJson, body, "Ожидается JSON-массив, содержащий список фильмов");
    }

    @Test
    void getMovieByYear_whenYearNotANumber_returnsBadRequestError() throws IOException, InterruptedException {
        Movie movie1 = new Movie("Крестный отец", 1972);
        Movie movie2 = new Movie("Дюна", 2021);
        movies.put(1, movie1);
        movies.put(2, movie2);

        HttpRequest resReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=YYYY"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(resReq, HttpResponse.BodyHandlers.ofString(UTF_8));

        assertEquals(400, resp.statusCode(), "GET /movies?year=YYYY должен вернуть 400");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        ErrorResponse errorResponse = gson.fromJson(body, ErrorResponse.class);

        assertEquals("Некорректный параметр запроса — 'year'", errorResponse.getError(),
                "В ответе должно быть сообщение об ошибке 'Некорректный параметр запроса — 'year''");
    }

    @Test
    void putMovies_returnsMethodNotAllowedError() throws IOException, InterruptedException {
        HttpRequest resReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .PUT(HttpRequest.BodyPublishers.ofString(""))
                .build();

        HttpResponse<String> resp = client.send(resReq, HttpResponse.BodyHandlers.ofString(UTF_8));

        assertEquals(405, resp.statusCode(), "GET /movies?year=YYYY должен вернуть 405");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        ErrorResponse errorResponse = gson.fromJson(body, ErrorResponse.class);

        assertEquals("Неподдерживаемый метод", errorResponse.getError(),
                "В ответе должно быть сообщение об ошибке 'Неподдерживаемый метод'");
    }
}