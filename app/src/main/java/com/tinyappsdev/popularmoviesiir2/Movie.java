package com.tinyappsdev.popularmoviesiir2;


class Movie implements java.io.Serializable {
    public int id;
    public String poster_path;
    public String title;
    public String vote_average;
    public String overview;
    public String release_date;

    static class Trailer {
        String id;
        String key;
        String name;
        String Site;
    }

    static class Review {
        String id;
        String author;
        String content;
    }
}
