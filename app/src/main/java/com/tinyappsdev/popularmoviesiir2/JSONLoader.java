package com.tinyappsdev.popularmoviesiir2;

import android.os.Handler;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.zip.GZIPInputStream;

/**
 * Created by pk on 4/23/2016.
 */
public class JSONLoader {
    static String TAG = JSONLoader.class.getName();
    private static JSONLoader _mainInstance;

    private int mNumWorker;
    private BlockingQueue<Request> mQueue;
    private boolean isDone;
    private Thread mThread[];

    public JSONLoader() {
        this(3);
    }

    public static JSONLoader getMainInstance() {
        if(_mainInstance != null) return _mainInstance;

        _mainInstance = new JSONLoader();
        return _mainInstance;
    }

    public JSONLoader(int numWorker) {
        mQueue = new LinkedBlockingDeque<Request>();
        mNumWorker = numWorker;

        mThread = new Thread[mNumWorker];
        for(int i = 0; i < mNumWorker; i++) {
            mThread[i] = new Worker();
            mThread[i].start();
        }
    }

    public void close() {
        isDone = true;
        for(int i = 0; i < mNumWorker; i++)
            mThread[i].interrupt();
    }

    public boolean addRequest(Request req) {
        try {
            mQueue.put(req);
        } catch(InterruptedException ex) {
            return false;
        }

        return true;
    }

    class Worker extends Thread {
        public Worker() {
            setDaemon(true);
        }

        public void run() {
            while(!isDone) {
                try {
                    consume(mQueue.take());
                } catch(InterruptedException ex) {

                }
            }
        }

        private void consume(Request req) {
            Object result = null;
            int errno = -1;

            if(!isDone && !req.isCancelled()) {
                req.attach(this);
                try {
                    result = req.OnReady(fetchData(req));
                    errno = 0;
                } catch (Exception ex) {
                    Log.i("PK", ex.toString());
                }
                req.detach();
            }

            if(!isDone && !req.isCancelled())
                req.postResult(errno, result);

        }

        private JSONObject fetchData(Request req) throws Exception {
            HttpURLConnection conn = (HttpURLConnection) (new URL(req.mUrl)).openConnection();
            conn.setRequestProperty("Accept-Encoding", "gzip");

            String enc = conn.getContentEncoding();
            InputStream ins = conn.getInputStream();
            if(enc != null && enc.toLowerCase().indexOf("gzip") >= 0) {
                ins = new GZIPInputStream(ins);
            }

            BufferedReader rd = new BufferedReader(new InputStreamReader(ins, "UTF-8"));
            StringBuilder result = new StringBuilder();
            String line;
            while((line = rd.readLine()) != null)
                result.append(line);
            rd.close();

            return new JSONObject(result.toString());
        }

    }

    public abstract static class Request {
        private boolean mIsCancelled;
        private Thread mCurThread;
        private Handler mHandler;
        private boolean mIsFinished;
        private Object _mLock;
        private String mUrl;

        public Request() {
            _mLock = new Object();
            mHandler = new Handler();
        }

        public void setUrl(String url) {
            mUrl = url;
        }

        public boolean isCancelled() {
            return mIsCancelled;
        }

        public boolean isFinished() {
            return mIsFinished;
        }

        public void cancel() {
            if(isFinished() || isCancelled()) return;

            mIsCancelled = true;
            mIsFinished = true;

            synchronized(_mLock) {
                if(mCurThread != null) {
                    try {
                        mCurThread.interrupt();
                    } catch (Exception ex) {
                        //bypass
                    }
                    mCurThread = null;
                }
            }
        }

        private void attach(Thread thread) {
            synchronized(_mLock) {
                mCurThread = thread;
            }
        }

        private void detach() {
            synchronized(_mLock) {
                mCurThread = null;
            }
        }

        private void postResult(final int errno, final Object result) {
            mHandler.post(new Runnable() {
                public void run() {
                    if(!isCancelled()) onResult(errno, result);
                    mIsFinished = true;

                    Log.i(TAG, String.format("postResult(errno:%d, result:%s) -> %s", errno,
                            result != null ? result.hashCode() : "NULL", mUrl)
                    );

                }
            });
        }

        protected abstract Object OnReady(JSONObject json) throws Exception;
        protected abstract void onResult(int errno, Object result);
    }

}

