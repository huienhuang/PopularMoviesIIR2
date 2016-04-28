package com.tinyappsdev.popularmoviesiir2;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    final static public String TAG = MainActivity.class.getName();
    final static public int MIN_ITEMS = 10;

    static private MovieList _lastMovieList;

    private JSONLoader mLoader;
    private FavoriteMovies mFavoriteMovies;
    private Button mButtonLoadMore;
    private GridView mGridViewMovieList;
    private ArrayAdapter<Movie> mAdapterMovie;
    private boolean mIsMultiPanel;
    private FetchMovieDataTask mFetchMovieDataTask;
    private MovieList mMovieList;
    private Runnable mOnChangedFavoriteMovie;

    class MovieList {
        ArrayList<Movie> list = new ArrayList<Movie>();
        int sorting_index = -1;
        int cur_page;
        int max_page;
        int cur_selection;
        int cur_position;
    }

    public void onClick_LoadMore(View view) {
        fetchMovieData();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mMovieList.cur_position = mGridViewMovieList.getFirstVisiblePosition();
        super.onSaveInstanceState(outState);
    }

    private void restoreMovieList() {
        if(_lastMovieList == null) {
            mMovieList = new MovieList();
        } else {
            mMovieList = _lastMovieList;
            _lastMovieList = null;
        }
    }

    private void saveMovieList() {
        _lastMovieList = mMovieList;
        mMovieList = null;
    }

    public MainActivity() {
        mLoader = JSONLoader.getMainInstance();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Context ctx = this;
        restoreMovieList();

        View detail_layout = findViewById(R.id.framelayout_movie_detail);
        mIsMultiPanel = detail_layout != null && detail_layout.getVisibility() == View.VISIBLE;

        mButtonLoadMore = (Button)findViewById(R.id.button_load_more);
        mGridViewMovieList = (GridView)findViewById(R.id.gridview_movie_list);
        mGridViewMovieList.setOnScrollListener(
                new AbsListView.OnScrollListener() {
                    @Override
                    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                        if(totalItemCount > 0 && firstVisibleItem + visibleItemCount >= totalItemCount - MIN_ITEMS) {
                            fetchMovieData();
                        }
                    }
                    @Override
                    public void onScrollStateChanged(AbsListView view, int scrollState) {}
                }
        );

        mAdapterMovie = setup_movie_adapter(ctx, new ArrayList<Movie>(mMovieList.list));
        mGridViewMovieList.setAdapter(mAdapterMovie);

        mGridViewMovieList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                if(mMovieList.cur_selection != position) {
                    mMovieList.cur_selection = position;
                    mAdapterMovie.notifyDataSetChanged();
                }
                loadDetail(position);
            }
        });

        mOnChangedFavoriteMovie = new Runnable() {
            @Override
            public void run() {
                if(mMovieList.sorting_index == 2) {
                    mMovieList.cur_selection = -1;
                    mMovieList.list.clear();
                    mMovieList.list.addAll( mFavoriteMovies.getAll() );
                    mAdapterMovie.clear();
                    mAdapterMovie.addAll(mMovieList.list);
                }

                mAdapterMovie.notifyDataSetChanged();
            }
        };
        mFavoriteMovies = FavoriteMoviesDB.getMainInstance(ctx);
        mFavoriteMovies.registerNotifier(mOnChangedFavoriteMovie);

        onChangedSorting();

        if(mMovieList.cur_position >= 0 && mMovieList.cur_position < mAdapterMovie.getCount())
            mGridViewMovieList.smoothScrollToPosition(mMovieList.cur_position);

        if(mIsMultiPanel && mMovieList.cur_selection >= 0 && mMovieList.cur_selection < mAdapterMovie.getCount())
            loadDetail(mMovieList.cur_selection);
    }

    protected void loadDetail(int position) {
        Movie m = mAdapterMovie.getItem(position);
        setTitle(m.title);

        if (mIsMultiPanel) {
            MovieDetailFragment detail = (MovieDetailFragment)getFragmentManager().findFragmentById(R.id.framelayout_movie_detail);
            if(detail == null || detail.getArguments().getInt("position") != position) {
                Bundle args = new Bundle();
                args.putInt("position", position);
                args.putSerializable("movie", m);
                detail = new MovieDetailFragment();
                detail.setArguments(args);

                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.framelayout_movie_detail, detail);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }

        } else {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra("movie", m);
            startActivity(intent);
        }
    }


    private ArrayAdapter<Movie> setup_movie_adapter(final Context ctx, ArrayList<Movie> list) {

        ArrayAdapter<Movie> adapter = new ArrayAdapter<Movie>(ctx, R.layout.grid_item_movie, list) {

            class ViewHolder {
                ImageView poster;
                TextView score;
                ImageView favoriteIcon;
            }

            public View getView(int position, View convertView, ViewGroup parent) {
                Movie m = getItem(position);

                if(convertView == null) {
                    convertView = LayoutInflater.from(ctx).inflate(R.layout.grid_item_movie, parent, false);

                    ViewHolder holder = new ViewHolder();
                    holder.poster = (ImageView)convertView.findViewById(R.id.imageview_poster);
                    holder.score = (TextView)convertView.findViewById(R.id.textview_score);
                    holder.favoriteIcon = (ImageView)convertView.findViewById(R.id.imageView_favorite_icon);

                    convertView.setTag(holder);
                }

                ViewHolder holder = (ViewHolder)convertView.getTag();
                holder.score.setText(m.vote_average);
                Picasso.with(ctx).load(MovieApiUrlBuilder.getUrlMovieImage(m.poster_path)).into(holder.poster);

                //convertView.setForeground( mMovieList.cur_selection == position ? new ColorDrawable(0x603F7FBF) : null );
                if(mMovieList.cur_selection == position) {
                    holder.poster.setAlpha(0.7f);
                    holder.score.setTextColor(0xffFF0000);
                } else {
                    holder.poster.setAlpha(1.0f);
                    holder.score.setTextColor(0xffe5bd5e);
                }

                holder.favoriteIcon.setVisibility(mFavoriteMovies.isMarked(m) ? View.VISIBLE : View.INVISIBLE);

                return convertView;
            }
        };

        adapter.setNotifyOnChange(false);


        return adapter;
    }

    private String getSortingApiUrl(int pageNum) {
        String url = null;
        switch(mMovieList.sorting_index) {
            case 0:
                url = MovieApiUrlBuilder.getUrlPopular(pageNum);
                break;
            case 1:
                url = MovieApiUrlBuilder.getUrlTopRated(pageNum);
                break;
        }

        return url;
    }

    private void fetchMovieData() {
        int next_page = mMovieList.cur_page + 1;
        if(next_page > mMovieList.max_page) return;

        if(mMovieList.sorting_index == 2) {
            mMovieList.list.addAll(mFavoriteMovies.getAll());
            mAdapterMovie.addAll(mMovieList.list);
            mAdapterMovie.notifyDataSetChanged();
            mMovieList.max_page = 0;
            return;
        }

        if(mFetchMovieDataTask != null && !mFetchMovieDataTask.isFinished()) return;

        mFetchMovieDataTask = new FetchMovieDataTask();
        mFetchMovieDataTask.setUrl(getSortingApiUrl(next_page));
        mLoader.addRequest(mFetchMovieDataTask);
    }

    class FetchMovieDataTask extends JSONLoader.Request {

        protected Object OnReady(JSONObject obj) throws Exception {
            int page = obj.getInt("page");
            int total_pages = obj.getInt("total_pages");

            JSONArray results = obj.getJSONArray("results");
            final int len = Math.min(results.length(), 1000);

            Movie[] movies = new Movie[len];
            for(int i = 0; i < len; i++) {
                Movie m = new Movie();
                movies[i] = m;

                JSONObject o = results.getJSONObject(i);
                m.id = o.getInt("id");
                m.title = o.getString("title");
                m.overview = o.getString("overview");
                m.release_date = o.getString("release_date");
                m.vote_average = o.getString("vote_average");
                m.poster_path = o.getString("poster_path");
            }

            return new Object[] {page, total_pages, movies};
        }

        protected void onResult(int errno, Object res) {
            boolean err = true;
            Object result[] = (Object[])res;

            if(errno == 0) {
                List movies = Arrays.asList((Movie[]) result[2]);

                if((int)result[0] == mMovieList.cur_page + 1) {
                    err = false;
                    mMovieList.list.addAll(movies);
                    mMovieList.cur_page++;
                    mMovieList.max_page = (int) result[1];
                    mAdapterMovie.addAll(movies);
                    mAdapterMovie.notifyDataSetChanged();
                }
            }

            mButtonLoadMore.setVisibility(err ? View.VISIBLE : View.INVISIBLE);
        }

    };

    @Override
    protected void onDestroy() {
        mFavoriteMovies.unregisterNotifier(mOnChangedFavoriteMovie);
        if(mFetchMovieDataTask != null) mFetchMovieDataTask.cancel();
        saveMovieList();

        super.onDestroy();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_sorting:
                new SortingDialog().show(getFragmentManager(), "SortingDialog");
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void removeDetailFragment() {
        MovieDetailFragment detail = (MovieDetailFragment)getFragmentManager().findFragmentById(R.id.framelayout_movie_detail);
        if(detail != null) {
            getFragmentManager().beginTransaction().remove(detail).commit();
        }
    }

    private void onChangedSorting() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        int idx = Integer.parseInt(settings.getString("sorting", "0"));
        if(idx == mMovieList.sorting_index) return;

        if(mFetchMovieDataTask != null) mFetchMovieDataTask.cancel();
        removeDetailFragment();

        mMovieList = new MovieList();
        mMovieList.sorting_index = idx;
        mMovieList.cur_selection = -1;
        mMovieList.max_page = 1;

        mAdapterMovie.clear();
        mAdapterMovie.notifyDataSetChanged();
        mGridViewMovieList.smoothScrollToPosition(0);

        setTitle(getResources().getStringArray(R.array.sorting)[idx]);

        fetchMovieData();
    }

    public static class SortingDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
            final int idx = Integer.parseInt(settings.getString("sorting", "0"));

            DialogInterface.OnClickListener onclick = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();

                    if(idx != which) {
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString("sorting", "" + which);
                        editor.apply();

                        ((MainActivity)activity).onChangedSorting();
                    }
                }
            };

            return new AlertDialog.Builder(getActivity())
                    .setTitle("Sorting")
                    .setSingleChoiceItems(R.array.sorting, idx, onclick)
                    .create();
        }

    }

}
