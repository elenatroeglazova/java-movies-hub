package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.LinkedHashMap;

public class MoviesStore extends LinkedHashMap<Integer, Movie> {

    public MoviesStore() {
        super();
    }

    @Override
    public Movie put(Integer key, Movie value) {
        value.setId(key);
        return super.put(key, value);
    }
}