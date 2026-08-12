package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.*;

public class MoviesStore implements List<Movie> {
    ArrayList<Movie> movies;

    public MoviesStore() {
        movies = new ArrayList<>();
    }

    @Override
    public int size() {
        return movies.size();
    }

    @Override
    public boolean isEmpty() {
        return movies.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        Movie movie = (Movie) o;
        return movies.contains(movie);
    }

    @Override
    public Iterator<Movie> iterator() {
        return movies.iterator();
    }

    @Override
    public Object[] toArray() {
        return new Movie[0];
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return movies.toArray(a);
    }

    @Override
    public boolean add(Movie movie) {
        return movies.add(movie);
    }

    @Override
    public boolean remove(Object o) {
        Movie movie = (Movie) o;
        return movies.remove(movie);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends Movie> c) {
        return false;
    }

    @Override
    public boolean addAll(int index, Collection<? extends Movie> c) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {
        movies.clear();
    }

    @Override
    public Movie get(int index) {
        return movies.get(index);
    }

    @Override
    public Movie set(int index, Movie element) {
        return null;
    }

    @Override
    public void add(int index, Movie element) {

    }

    @Override
    public Movie remove(int index) {
        return null;
    }

    @Override
    public int indexOf(Object o) {
        return 0;
    }

    @Override
    public int lastIndexOf(Object o) {
        return 0;
    }

    @Override
    public ListIterator<Movie> listIterator() {
        return movies.listIterator();
    }

    @Override
    public ListIterator<Movie> listIterator(int index) {
        return movies.listIterator(index);
    }

    @Override
    public List<Movie> subList(int fromIndex, int toIndex) {
        return List.of();
    }
}