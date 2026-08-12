package ru.practicum.moviehub.model;

import java.util.Objects;

public class Movie {
    private String title;
    private int year;
    private int id;
    private static int count = 0;

    public Movie(String title, int year) {
        this.title = title;
        this.year = year;
    }

    public Movie(String title, int year, int id) {
        this.title = title;
        this.year = year;
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public int getYear() {
        return year;
    }

    public int getId() {
        return id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setId() {
        id = ++count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Movie movie = (Movie) o;
        return getYear() == movie.getYear() && Objects.equals(getTitle(), movie.getTitle());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTitle(), getYear());
    }
}