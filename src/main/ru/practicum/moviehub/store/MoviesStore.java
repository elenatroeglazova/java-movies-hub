package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.LinkedHashMap;

public class MoviesStore extends LinkedHashMap<Integer, Movie> {
    LinkedHashMap<Integer, Movie> movies;

    public MoviesStore() {
        super();
        movies = new LinkedHashMap<>();
    }
}