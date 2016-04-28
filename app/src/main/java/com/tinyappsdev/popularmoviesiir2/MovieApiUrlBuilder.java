package com.tinyappsdev.popularmoviesiir2;

/**
 * Created by pk on 4/21/2016.
 */
final public class MovieApiUrlBuilder {


    final static public String API_KEY = "";



    final static public String API_BASE_URL = "https://api.themoviedb.org/3/movie/";
    final static public String IMG_URL = "http://image.tmdb.org/t/p/w185%s";


    public static String getUrlMovieImage(String relPath) {
        return String.format(IMG_URL, relPath);
    }

    public static String getUrlMovieVideos(int id) {
        return String.format(API_BASE_URL + "%s/%s?api_key=%s", id, "videos", API_KEY);
    }

    public static String getUrlMovieReviews(int id, int pageNum) {
        return String.format(API_BASE_URL + "%s/%s?api_key=%s&page=%s", id, "reviews", API_KEY, pageNum);
    }

    public static String getUrlPopular(int pageNum) {
        return String.format(API_BASE_URL + "%s?api_key=%s&page=%s", "popular", API_KEY, pageNum);
    }

    public static String getUrlTopRated(int pageNum) {
        return String.format(API_BASE_URL + "%s?api_key=%s&page=%s", "top_rated", API_KEY, pageNum);
    }

}
