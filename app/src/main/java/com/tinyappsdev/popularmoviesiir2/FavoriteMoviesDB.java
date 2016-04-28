package com.tinyappsdev.popularmoviesiir2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by pk on 4/26/2016.
 */
interface FavoriteMovies {

    boolean mark(Movie movie);
    boolean unmark(Movie movie);
    boolean isMarked(Movie movie);
    public Collection<Movie> getAll();
    void registerNotifier(Runnable runnable);
    void unregisterNotifier(Runnable runnable);

}

public class FavoriteMoviesDB implements FavoriteMovies {
    private static FavoriteMovies _mainInstance;

    SQLiteDatabase mDB;
    LinkedHashMap<Integer, Movie> mCache;
    HashSet<Runnable> mNotifiers;

    FavoriteMoviesDB(Context ctx) {
        mNotifiers = new HashSet<Runnable>();
        mDB = new DBHelper(ctx).getWritableDatabase();
    }

    public static FavoriteMovies getMainInstance(Context ctx) {
        if(_mainInstance != null) return _mainInstance;

        _mainInstance = new FavoriteMoviesDB(ctx.getApplicationContext());
        return _mainInstance;
    }

    @Override
    public void registerNotifier(Runnable runnable) {
        mNotifiers.add(runnable);
    }

    @Override
    public void unregisterNotifier(Runnable runnable) {
        mNotifiers.remove(runnable);
    }

    private void notifyChanged() {
        for(Runnable runnable: mNotifiers) {
            runnable.run();
        }
    }

    private LinkedHashMap<Integer, Movie> getCache() {
        if(mCache == null) {
            mCache = new LinkedHashMap<Integer, Movie>();
            prepareCache();
        }

        return mCache;
    }

    private void prepareCache() {
        Cursor cursor = mDB.query(DBHelper.TABLE_NAME, null, null, null, null, null, "_id asc");
        while(cursor.moveToNext()) {
            Movie movie = new Movie();
            movie.id = cursor.getInt(1);
            movie.poster_path = cursor.getString(2);
            movie.title = cursor.getString(3);
            movie.vote_average = cursor.getString(4);
            movie.overview = cursor.getString(5);
            movie.release_date = cursor.getString(6);
            mCache.put(movie.id, movie);
        }

        cursor.close();
    }

    @Override
    public Collection<Movie> getAll() {
        List<Movie> lst = new ArrayList<Movie>(getCache().values());
        Collections.reverse(lst);
        return lst;
    }


    @Override
    public boolean isMarked(Movie movie) {
        return getCache().containsKey(movie.id);
    }

    public boolean mark(Movie movie) {
        ContentValues values = new ContentValues();
        values.put("id", movie.id);
        values.put("poster_path", movie.poster_path);
        values.put("title", movie.title);
        values.put("vote_average", movie.vote_average);
        values.put("overview", movie.overview);
        values.put("release_date", movie.release_date);

        boolean result = (mDB.insert(DBHelper.TABLE_NAME, "null", values) >= 0);
        if(result) {
            getCache().put(movie.id, movie);
            notifyChanged();
        }

        return result;
    }

    @Override
    public boolean unmark(Movie movie) {
        boolean result = (mDB.delete(DBHelper.TABLE_NAME, String.format("id=%s", movie.id), null) > 0);
        if(result) {
            getCache().remove(movie.id);
            notifyChanged();
        }

        return result;
    }
}

class DBHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "favorite_movies.db";
    public static final String TABLE_NAME = "movies";
    public static final String SQL_CREATE_TABLE = String.format("create table %s (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "id INTEGER UNIQUE," +
            "poster_path TEXT," +
            "title TEXT," +
            "vote_average TEXT," +
            "overview TEXT," +
            "release_date TEXT" +
            ")", TABLE_NAME);

    public static final String SQL_DROP_TABLE = String.format("drop table if exists %s", TABLE_NAME);


    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DROP_TABLE);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

}

