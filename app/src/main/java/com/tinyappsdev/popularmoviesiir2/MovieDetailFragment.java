package com.tinyappsdev.popularmoviesiir2;


import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;


/**
 * A simple {@link Fragment} subclass.
 */
public class MovieDetailFragment extends Fragment {

    private JSONLoader mLoader;
    private FavoriteMovies mFavoriteMovies;

    private JSONLoader.Request mRequestTrailers;
    private JSONLoader.Request mRequestReviews;

    private Movie mMovie;

    public MovieDetailFragment() {
    }

    private void loadReviews(final LinearLayout container) {
        mRequestReviews = new JSONLoader.Request() {
            @Override
            protected Object OnReady(JSONObject json) throws Exception {
                JSONArray results = json.getJSONArray("results");
                int rlen = Math.min(results.length(), 5);
                Movie.Review reviews[] = new Movie.Review[rlen];
                for(int i = 0; i < rlen; i++) {
                    Movie.Review review = reviews[i] = new Movie.Review();
                    JSONObject o = results.getJSONObject(i);
                    review.id = o.getString("id");
                    review.author = o.getString("author");
                    review.content = o.getString("content");
                }
                int total_results = json.getInt("total_results");

                return new Object[] {total_results, reviews};
            }

            @Override
            protected void onResult(int errno, Object _result) {
                final Context ctx = getActivity();

                if(errno != 0) {
                    Toast.makeText(ctx, "Loading Error", Toast.LENGTH_SHORT).show();
                    return;
                }

                Object[] result = (Object[])_result;
                final Movie.Review reviews[] = (Movie.Review[])result[1];
                if(reviews.length == 0)  {
                    container.setVisibility(View.GONE);
                    return;
                }

                LayoutInflater inflater = LayoutInflater.from(ctx);
                ViewGroup layout = (ViewGroup)container.findViewById(R.id.layout_reviews_body);
                for(int i = 0; i < reviews.length; i++) {
                    Movie.Review review = reviews[i];
                    View view = inflater.inflate(R.layout.list_item_review, null, false);
                    ((TextView)view.findViewById(R.id.textView_review_author)).setText(review.author + ":");
                    ((TextView)view.findViewById(R.id.textView_review_content)).setText(review.content);

                    layout.addView(view);
                }

                Button buttonReadAllReviews = (Button)container.findViewById(R.id.button_read_all_reviews);
                buttonReadAllReviews.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Intent intent = new Intent(ctx, ReviewsActivity.class);
                        intent.putExtra("movie", mMovie);
                        startActivity(intent);
                    }
                });
                int total_results = (Integer)result[0];
                buttonReadAllReviews.setVisibility(total_results > reviews.length ? View.VISIBLE : View.GONE);

            }
        };

        mRequestReviews.setUrl( MovieApiUrlBuilder.getUrlMovieReviews(mMovie.id, 1) );
        mLoader.addRequest(mRequestReviews);
    }

    private void loadTrailers(final LinearLayout container) {
        mRequestTrailers = new JSONLoader.Request() {
            @Override
            protected Object OnReady(JSONObject json) throws Exception {
                JSONArray results = json.getJSONArray("results");
                Movie.Trailer trailers[] = new Movie.Trailer[results.length()];
                for(int i = 0; i < results.length(); i++) {
                    Movie.Trailer trailer = trailers[i] = new Movie.Trailer();
                    JSONObject o = results.getJSONObject(i);
                    trailer.id = o.getString("id");
                    trailer.key = o.getString("key");
                    trailer.name = o.getString("name");
                    trailer.Site = o.getString("site");
                }

                return trailers;
            }

            @Override
            protected void onResult(int errno, Object result) {
                Context ctx = getActivity();
                if(errno != 0) {
                    Toast.makeText(ctx, "Loading Error", Toast.LENGTH_SHORT).show();
                    return;
                }

                final Movie.Trailer trailers[] = (Movie.Trailer[])result;
                if(trailers.length == 0) {
                    container.setVisibility(View.GONE);
                    return;
                }

                LayoutInflater inflater = LayoutInflater.from(ctx);
                for(int i = 0; i < trailers.length; i++) {
                    Movie.Trailer trailer = trailers[i];
                    View view = inflater.inflate(R.layout.list_item_trailer, container, true);
                    Button button = (Button)view.findViewById(R.id.button_play_trailer);
                    button.setId(R.id.id_button_play_trailer_tmp);
                    button.setText(trailer.name);
                    button.setTag(trailers[i]);

                    button.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            Movie.Trailer trailer = (Movie.Trailer)v.getTag();

                            String url = "https://www.youtube.com/watch?v=" + trailer.key;
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(url));
                            startActivity(intent);
                        }
                    });
                }

            }
        };

        mRequestTrailers.setUrl( MovieApiUrlBuilder.getUrlMovieVideos(mMovie.id) );
        mLoader.addRequest(mRequestTrailers);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View view = inflater.inflate(R.layout.fragment_movie_detail, container, false);

        Activity activity = getActivity();
        mLoader = JSONLoader.getMainInstance();
        mFavoriteMovies = FavoriteMoviesDB.getMainInstance(activity);

        Bundle args = getArguments();
        mMovie = (Movie)args.getSerializable("movie");
        if(mMovie == null) return view;

        activity.setTitle(mMovie.title);

        ImageView img = (ImageView) view.findViewById(R.id.imageview_detail_poster);
        Picasso.with(activity).load(MovieApiUrlBuilder.getUrlMovieImage(mMovie.poster_path)).into(img);

        ((TextView) view.findViewById(R.id.textview_detail_date)).setText(mMovie.release_date);
        ((TextView) view.findViewById(R.id.textview_detail_score)).setText(mMovie.vote_average);
        ((TextView) view.findViewById(R.id.textview_detail_review)).setText(mMovie.overview);


        loadTrailers((LinearLayout)view.findViewById(R.id.layout_trailers));
        loadReviews((LinearLayout)view.findViewById(R.id.layout_reviews));


        Button buttonMaskAsFavorite = ((Button)view.findViewById(R.id.button_mask_as_favorite));
        boolean isMarked = mFavoriteMovies.isMarked(mMovie);
        buttonMaskAsFavorite.setText(isMarked ? "Unmark" : "Mark As Favorite");

        buttonMaskAsFavorite.setTag(isMarked);
        buttonMaskAsFavorite.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean isMarked = (Boolean) v.getTag();
                if(isMarked)
                    mFavoriteMovies.unmark(mMovie);
                else
                    mFavoriteMovies.mark(mMovie);
                isMarked = mFavoriteMovies.isMarked(mMovie);
                v.setTag(isMarked);
                ((Button)v).setText(isMarked ? "Unmark" : "Mark As Favorite");
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        if(mRequestTrailers != null) mRequestTrailers.cancel();
        if(mRequestReviews != null) mRequestReviews.cancel();

        super.onDestroyView();
    }
}
