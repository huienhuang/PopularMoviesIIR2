package com.tinyappsdev.popularmoviesiir2;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class ReviewsActivity extends AppCompatActivity {
    private JSONLoader mLoader;

    final static public int MIN_ITEMS = 10;

    private ListView mListViewReviews;
    private ArrayAdapter<Movie.Review> mAdapter;
    private JSONLoader.Request mCurRequest;
    private Movie mMovie;
    private int mCurPageNum;
    private int mMaxPageNum;

    public ReviewsActivity() {
        mMaxPageNum = 1;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviews);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mLoader = JSONLoader.getMainInstance();
        mMovie = (Movie)getIntent().getSerializableExtra("movie");

        mListViewReviews = (ListView)findViewById(R.id.listView_reviews);
        mListViewReviews.setOnScrollListener(
                new AbsListView.OnScrollListener() {
                    @Override
                    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                        if(firstVisibleItem + visibleItemCount >= totalItemCount - MIN_ITEMS) {
                            loadReviews();
                        }
                    }
                    @Override
                    public void onScrollStateChanged(AbsListView view, int scrollState) {}
                }
        );

        final LayoutInflater inflater = LayoutInflater.from(this);
        mAdapter = new ArrayAdapter<Movie.Review>(this, R.layout.list_item_review, new ArrayList<Movie.Review>()) {
            class ViewHolder {
                TextView author;
                TextView content;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                Movie.Review review = getItem(position);

                if(convertView == null) {
                    convertView = inflater.inflate(R.layout.list_item_review, parent, false);
                    ViewHolder holder = new ViewHolder();
                    holder.author = (TextView)convertView.findViewById(R.id.textView_review_author);
                    holder.content = (TextView)convertView.findViewById(R.id.textView_review_content);
                    convertView.setTag(holder);
                }

                ViewHolder holder = (ViewHolder)convertView.getTag();
                holder.author.setText(review.author + ":");
                holder.content.setText(review.content);

                return convertView;
            }
        };

        mListViewReviews.setAdapter(mAdapter);
    }

    public void loadReviews() {
        if(mCurPageNum >= mMaxPageNum) return;
        if(mCurRequest != null && !mCurRequest.isFinished()) return;

        mCurRequest = new ReviewRequest();
        mCurRequest.setUrl( MovieApiUrlBuilder.getUrlMovieReviews(mMovie.id, mCurPageNum + 1) );
        mLoader.addRequest(mCurRequest);
    }

    @Override
    protected void onDestroy() {
        if(mCurRequest != null) mCurRequest.cancel();

        super.onDestroy();
    }

    class ReviewRequest extends JSONLoader.Request {
        @Override
        protected Object OnReady(JSONObject json) throws Exception {
            JSONArray results = json.getJSONArray("results");
            int rlen = results.length();
            Movie.Review reviews[] = new Movie.Review[rlen];
            for(int i = 0; i < rlen; i++) {
                Movie.Review review = reviews[i] = new Movie.Review();
                JSONObject o = results.getJSONObject(i);
                review.id = o.getString("id");
                review.author = o.getString("author");
                review.content = o.getString("content");
            }
            int total_pages = json.getInt("total_pages");

            return new Object[] {total_pages, reviews};
        }

        @Override
        protected void onResult(int errno, Object _result) {
            if(errno != 0) return;

            Object[] result = (Object[])_result;
            Movie.Review reviews[] = (Movie.Review[])result[1];
            mCurPageNum++;
            mMaxPageNum = (Integer)result[0];
            mAdapter.addAll( Arrays.asList(reviews) );
        }
    }

}
