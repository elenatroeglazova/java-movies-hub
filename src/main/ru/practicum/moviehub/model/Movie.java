package ru.practicum.moviehub.model;

import java.util.Objects;

public class Movie {
    private String title;
    private int year;
    private int id;

    public Movie(String title, int year) {
        this.title = title;
        this.year = year;
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

    public void setId(int id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setYear(int year) {
        this.year = year;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Movie movie = (Movie) o;
        return getYear() == movie.getYear() &&
                Objects.equals(getTitle(), movie.getTitle()) &&
                getId() == movie.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTitle(), getYear(), getId());
    }
}